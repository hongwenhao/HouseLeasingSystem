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
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import NavBar from '../components/NavBar.vue'
import Footer from '../components/Footer.vue'
import { verifyAlipaySyncReturn } from '../api/alipay.js'

const route = useRoute()
const router = useRouter()
const resultSuccess = ref(false)
const resultMessage = ref('正在确认支付结果，请稍候...')
const orderId = ref(null)

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
      map[key] = val[0] ?? ''
    } else {
      map[key] = val == null ? '' : String(val)
    }
  }
  return map
}

onMounted(async () => {
  try {
    const params = normalizeQueryToMap()
    const res = await verifyAlipaySyncReturn(params)
    resultSuccess.value = !!res?.success
    resultMessage.value = res?.message || (resultSuccess.value ? '支付成功' : '支付确认失败')
    orderId.value = res?.orderId || null
    if (resultSuccess.value) {
      ElMessage.success('支付已确认')
    }
  } catch (e) {
    resultSuccess.value = false
    resultMessage.value = e.message || '支付确认失败'
  }
})

/** 返回个人中心并定位到“预约订单管理”标签页 */
function goUserCenter() {
  router.push('/user-center?tab=orders')
}

/** 跳转到订单详情页 */
function goOrderDetail() {
  if (!orderId.value) return
  router.push(`/orders/${orderId.value}`)
}
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
