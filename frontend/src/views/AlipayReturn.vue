<template>
  <div class="alipay-return-page">
    <NavBar />
    <div class="page-content">
      <el-card class="result-card">
        <template #header>支付宝支付结果</template>
        <div class="result-body">
          <el-result
            :icon="resultSuccess ? 'success' : 'error'"
            :title="resultSuccess ? '支付已确认' : '支付确认失败'"
            :sub-title="resultMessage"
          />
          <div class="actions">
            <el-button type="primary" @click="goUserCenter">返回个人中心</el-button>
            <el-button v-if="orderId" @click="goOrderDetail">查看订单</el-button>
          </div>
        </div>
      </el-card>
    </div>
    <Footer />
  </div>
</template>

<script setup>
/**
 * AlipayReturn.vue —— 支付宝同步回跳页
 *
 * 职责：
 * 1) 接收支付宝 return_url query 参数；
 * 2) 调后端接口完成验签、金额校验和订单支付状态落库；
 * 3) 向用户展示最终结果，并提供返回入口。
 */
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import NavBar from '../components/NavBar.vue'
import Footer from '../components/Footer.vue'
import { verifyAlipaySyncReturn } from '../api/alipay.js'
import { getOrderDetail } from '../api/order.js'

const route = useRoute()
const router = useRouter()
const resultSuccess = ref(false)
const resultMessage = ref('正在确认支付结果，请稍候...')
const orderId = ref(null)
// 支付成功后给用户一个短暂“已确认”反馈，再自动跳转订单页（单位：毫秒）。
const PAYMENT_SUCCESS_REDIRECT_DELAY = 800
// 支付状态兜底轮询：最多轮询 6 次，每次间隔 2 秒（约 12 秒）。
const PAYMENT_STATUS_POLL_INTERVAL = 2000
const PAYMENT_STATUS_POLL_MAX_ATTEMPTS = 6
let pollTimer = null

/**
 * 提取路由 query，统一转为后端需要的字符串键值对
 *
 * @returns {Record<string, string>}
 */
function normalizeQueryToMap() {
  const map = {}
  const query = route.query || {}
  for (const [key, val] of Object.entries(query)) {
    if (Array.isArray(val)) {
      // 理论上支付宝回调参数均应为单值；若出现数组，记录告警并取首值，避免参数结构异常导致流程中断。
      console.warn('[AlipayReturn] 回调参数出现数组值，将使用首个元素：', key, val)
      map[key] = normalizeAlipayCallbackValue(key, val[0] ?? '')
    } else {
      map[key] = normalizeAlipayCallbackValue(key, val == null ? '' : String(val))
    }
  }
  return map
}

/**
 * 统一规范化支付宝回调参数值
 *
 * 为什么需要这一步：
 * - Vue Router 解析 query 时会执行 URL 解码；
 * - 在部分浏览器/网关组合下，`sign` 中的 `+` 可能被还原为空格；
 * - 但支付宝验签要求签名串与原始值严格一致，`+` 被改成空格会直接验签失败。
 *
 * 后果：
 * - 验签失败后，后端不会执行订单支付落库；
 * - 前端因此拿不到“支付确认成功”，也不会自动跳转“预约订单管理”。
 *
 * 处理策略：
 * - 仅对 `sign` 字段执行“空格 -> +”兼容修复；
 * - 其他字段保持原值，避免误改交易号、金额等业务参数。
 *
 * @param {string} key 参数名
 * @param {string} value 参数值
 * @returns {string} 修复后的参数值
 */
function normalizeAlipayCallbackValue(key, value) {
  if (key !== 'sign') return value
  return value.replace(/ /g, '+')
}

onMounted(async () => {
  try {
    const params = normalizeQueryToMap()
    // 兜底校验：若支付宝未回传关键参数，直接提示并引导用户回订单页，避免出现“空白成功页”的误导体验。
    if (!params.out_trade_no) {
      throw new Error('未获取到订单号，请返回订单列表刷新后查看支付状态')
    }
    const res = await verifyAlipaySyncReturn(params)
    resultSuccess.value = !!res?.success
    resultMessage.value = res?.message || (resultSuccess.value ? '支付成功' : '支付确认失败')
    orderId.value = res?.orderId || null
    if (resultSuccess.value) {
      ElMessage.success('支付已确认')
      // 兜底：支付确认后再轮询一次订单支付状态，确保“已支付”状态真正可见后再跳转。
      await pollOrderPaymentStatusAndRedirect()
    }
  } catch (e) {
    resultSuccess.value = false
    resultMessage.value = e.message || '支付确认失败'
  }
})

/** 返回个人中心并定位到“预约订单管理”标签页 */
function goUserCenter() {
  // 手动返回也统一带 fromPay 标记，确保订单列表刷新为最新支付结果。
  router.push('/user-center?tab=orders&fromPay=1')
}

/** 跳转到订单详情页 */
function goOrderDetail() {
  if (!orderId.value) return
  router.push(`/orders/${orderId.value}`)
}

/**
 * 轮询订单支付状态并在确认后跳转个人中心
 *
 * 设计目的：
 * 1) 解决极端情况下“验签接口刚返回成功，但订单列表刷新仍短暂显示未支付”的瞬时不一致；
 * 2) 若轮询超时，给出清晰提示并保留手动入口，不阻塞用户继续操作。
 */
async function pollOrderPaymentStatusAndRedirect() {
  if (!orderId.value) {
    // 没有订单 ID 时无法做精确轮询，直接按原路径跳转。
    scheduleGoUserCenter()
    return
  }

  let attempts = 0
  const runCheck = async () => {
    attempts += 1
    try {
      const detail = await getOrderDetail(orderId.value)
      if (detail?.paymentStatus === 'PAID') {
        clearPollTimer()
        scheduleGoUserCenter()
        return
      }
    } catch (e) {
      // 轮询失败不立即中断，继续在剩余次数内重试，降低瞬时网络抖动影响。
    }

    if (attempts >= PAYMENT_STATUS_POLL_MAX_ATTEMPTS) {
      clearPollTimer()
      resultMessage.value = '支付已确认，但订单状态同步稍慢，请返回“预约订单管理”刷新查看'
      return
    }
    pollTimer = setTimeout(runCheck, PAYMENT_STATUS_POLL_INTERVAL)
  }

  await runCheck()
}

/** 统一调度跳转，避免重复硬编码路由和延时逻辑。 */
function scheduleGoUserCenter() {
  setTimeout(() => {
    router.replace('/user-center?tab=orders&fromPay=1')
  }, PAYMENT_SUCCESS_REDIRECT_DELAY)
}

/** 清理轮询定时器，防止组件销毁后仍继续执行异步轮询。 */
function clearPollTimer() {
  if (!pollTimer) return
  clearTimeout(pollTimer)
  pollTimer = null
}

onBeforeUnmount(() => {
  clearPollTimer()
})
</script>

<style scoped>
.alipay-return-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.page-content {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px;
}

.result-card {
  width: 100%;
  max-width: 640px;
  border-radius: 12px;
}

.result-body {
  padding: 8px 0;
}

.actions {
  margin-top: 16px;
  display: flex;
  justify-content: center;
  gap: 12px;
}
</style>
