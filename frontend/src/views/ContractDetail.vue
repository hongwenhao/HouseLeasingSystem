<template>
  <div class="contract-detail-page">
    <NavBar />
    <div class="page-content" v-if="contract">
      <div class="page-inner">
        <div class="page-header">
          <el-button @click="$router.back()" text class="back-btn">
            <el-icon><ArrowLeft /></el-icon> 返回
          </el-button>
          <h2 class="page-title">合同详情</h2>
          <div class="header-actions">
            <el-button size="default" @click="openWorkflowDialog">
              查看流程图
            </el-button>
            <el-button size="default" @click="handleDownloadPdf">
              <el-icon><Download /></el-icon> 下载PDF
            </el-button>
            <el-tag :type="statusType" size="large" effect="light">{{ statusLabel }}</el-tag>
          </div>
        </div>

        <!-- Contract Content -->
        <el-card class="contract-card">
          <template #header><div class="card-main-title">房屋租赁合同</div></template>
          <div class="parties-grid">
            <div class="party-card landlord">
              <h4>🏠 出租方（房东）</h4>
              <div class="party-row">
                <span>姓名</span>
                <strong>{{ contract.landlord?.realName || '-' }}</strong>
              </div>
              <div class="party-row">
                <span>手机号</span>
                <strong>{{ contract.landlord?.phone || '-' }}</strong>
              </div>
              <div class="party-row">
                <span>身份证号</span>
                <strong>{{ contract.landlord?.idCard || '-' }}</strong>
              </div>
              <div class="party-row">
                <span>房东类型</span>
                <strong>{{ ownerTypeLabel(contract.house?.ownerType) }}</strong>
              </div>
              <div class="party-row">
                <span>信用分</span>
                <strong>{{ contract.landlord?.creditScore ?? '-' }}</strong>
              </div>
            </div>
            <div class="party-card tenant">
              <h4>🧍 承租方（租客）</h4>
              <div class="party-row">
                <span>姓名</span>
                <strong>{{ contract.tenant?.realName || '-' }}</strong>
              </div>
              <div class="party-row">
                <span>手机号</span>
                <strong>{{ contract.tenant?.phone || '-' }}</strong>
              </div>
              <div class="party-row">
                <span>身份证号</span>
                <strong>{{ contract.tenant?.idCard || '-' }}</strong>
              </div>
              <div class="party-row">
                <span>信用分</span>
                <strong>{{ contract.tenant?.creditScore ?? '-' }}</strong>
              </div>
            </div>
          </div>

          <div class="section-block">
            <h3>租赁房屋信息</h3>
            <el-descriptions :column="1" size="default">
              <el-descriptions-item label="合同编号">{{ contract.contractNo || contract.id }}</el-descriptions-item>
              <el-descriptions-item label="签订日期">{{ formatDate(contract.signTime || contract.createTime, true) }}</el-descriptions-item>
              <el-descriptions-item label="租赁期限">
                {{ formatDate(contract.startDate) }} 至 {{ formatDate(contract.endDate) }}
              </el-descriptions-item>
              <el-descriptions-item label="月租金">
                <span class="rent-highlight">¥{{ contract.monthlyRent ?? contract.rent }}/月</span>
              </el-descriptions-item>
              <el-descriptions-item label="押金">¥{{ contract.deposit }}</el-descriptions-item>
              <el-descriptions-item label="关联订单编号">
                {{ contract.orderNo || contract.orderId || '-' }}
              </el-descriptions-item>
            </el-descriptions>
            <div class="section-actions">
              <el-button
                v-if="contract.orderId"
                size="small"
                type="primary"
                plain
                @click="$router.push(`/orders/${contract.orderId}`)"
              >
                查看对应订单
              </el-button>
            </div>
          </div>

          <div class="section-block" v-if="clauseList.length">
            <h3>主要条款</h3>
            <ul class="clause-list">
              <li v-for="(clause, idx) in clauseList" :key="idx">{{ clause }}</li>
            </ul>
          </div>

          <div class="signature-grid">
            <div class="signature-card">
              <div class="sig-title">房东签署</div>
              <div class="sig-name">{{ contract.landlord?.realName || '-' }}</div>
              <div class="sig-time">{{ formatDate(contract.landlordSignTime, true) }} {{ contract.landlordSigned ? '✅ 已签署' : '⏳ 待签署' }}</div>
            </div>
            <div class="signature-card">
              <div class="sig-title">租客签署</div>
              <div class="sig-name">{{ contract.tenant?.realName || '-' }}</div>
              <div class="sig-time">{{ formatDate(contract.tenantSignTime, true) }} {{ contract.tenantSigned ? '✅ 已签署' : '⏳ 待签署' }}</div>
            </div>
          </div>
        </el-card>

        <!-- Actions -->
        <div class="action-section" v-if="showSignBtn || showCancelBtn || landlordNeedWaitHint">
          <el-alert
            v-if="landlordNeedWaitHint"
            type="warning"
            show-icon
            :closable="false"
            class="landlord-wait-alert"
            title="请等待租客先签署合同，房东才能签署合同。"
          />
          <el-button
            v-if="showSignBtn"
            type="primary"
            size="large"
            :loading="actioning"
            :disabled="landlordNeedWaitHint"
            @click="handleSign"
          >
            <el-icon><Edit /></el-icon> {{ landlordNeedWaitHint ? '等待租客先签署' : '签署合同' }}
          </el-button>
          <el-button
            v-if="showCancelBtn"
            type="danger"
            size="large"
            @click="cancelDialogVisible = true"
          >
            取消合同
          </el-button>
        </div>

        <el-card class="risk-card">
          <template #header>
            <div class="risk-title">🔍 合同风险分析</div>
          </template>
          <RiskWarning :risks="risks" />
        </el-card>
      </div>
    </div>
    <div v-else-if="loading" class="loading-wrap">
      <el-skeleton :rows="10" animated />
    </div>
    <el-empty v-else description="合同不存在" />

    <!-- Cancel Dialog -->
    <el-dialog v-model="cancelDialogVisible" title="取消合同" width="400px">
      <p>确认取消该合同吗？草稿或待签署阶段可取消，取消后合同状态将变为“已取消”。</p>
      <template #footer>
        <el-button @click="cancelDialogVisible = false">返回</el-button>
        <el-button type="danger" :loading="actioning" @click="handleCancel">确认取消</el-button>
      </template>
    </el-dialog>

    <!-- Workflow Monitor Dialog -->
    <el-dialog v-model="workflowDialogVisible" title="合同签署流程图监控" width="900px" destroy-on-close>
      <div class="workflow-toolbar">
        <el-tag type="info" effect="light">实时刷新：每 10 秒自动同步一次</el-tag>
        <el-button size="small" :loading="workflowLoading" @click="loadWorkflowMonitor(true)">立即刷新</el-button>
      </div>
      <el-skeleton v-if="workflowLoading && !workflowMonitor" :rows="6" animated />
      <template v-else>
        <el-alert
          v-if="workflowMonitor"
          :type="workflowMonitor.finished ? 'success' : 'warning'"
          :closable="false"
          show-icon
          class="workflow-alert"
          :title="workflowMonitor.finished
            ? '流程已结束'
            : `当前停留节点：${workflowMonitor.currentNodeName || '未识别'}`"
          :description="`最近同步时间：${formatDate(workflowMonitor.queryTime, true)}`"
        />
        <el-steps
          v-if="workflowNodes.length"
          :active="workflowActiveIndex"
          align-center
          finish-status="success"
          process-status="process"
          class="workflow-steps"
        >
          <el-step
            v-for="(node, idx) in workflowNodes"
            :key="node.nodeId || idx"
            :title="node.nodeName || `节点${idx + 1}`"
            :status="workflowStepStatus(node.status)"
            :description="node.shortDesc"
          />
        </el-steps>
        <el-empty v-else description="暂未获取到流程节点信息" />
        <div v-if="workflowNodes.length" class="workflow-node-list">
          <div v-for="(node, idx) in workflowNodes" :key="`${node.nodeId}-${idx}`" class="workflow-node-item">
            <div class="node-title">
              <span>{{ idx + 1 }}. {{ node.nodeName || '-' }}</span>
              <el-tag size="small" :type="workflowTagType(node.status)">{{ workflowStatusLabel(node.status) }}</el-tag>
            </div>
            <div class="node-meta">
              处理人：{{ node.assigneeName || node.assigneeId || '-' }} ｜ 节点停留：{{ node.stayText }}
            </div>
            <div class="node-meta">
              进入时间：{{ formatDate(node.enterTime, true) }} ｜ 完成时间：{{ formatDate(node.completeTime, true) }}
            </div>
          </div>
        </div>
      </template>
    </el-dialog>

    <Footer />
  </div>
</template>

<script setup>
// 说明：合同详情页逻辑，展示合同完整信息、AI 风险检测结果，并支持签署和取消合同操作
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import NavBar from '../components/NavBar.vue'
import Footer from '../components/Footer.vue'
import RiskWarning from '../components/RiskWarning.vue'
import {
  getContractDetail,
  signContract,
  cancelContract,
  getContractRisks,
  downloadContractPdf,
  getContractWorkflowMonitor
} from '../api/contract.js'

const route = useRoute()
const loading = ref(false)                  // 页面加载状态
const actioning = ref(false)               // 签署/终止按钮 loading 状态
const contract = ref(null)                 // 合同详情数据
const risks = ref([])                      // AI 检测到的合同风险列表
const cancelDialogVisible = ref(false)  // 取消合同对话框显隐
const role = localStorage.getItem('role') || ''  // 当前用户角色
const workflowDialogVisible = ref(false)   // 流程图监控弹窗显隐
const workflowLoading = ref(false)         // 流程图监控加载状态
const workflowMonitor = ref(null)          // 后端返回的流程图监控数据
const workflowRefreshTimer = ref(null)     // 10 秒一次的自动拉取定时器
const workflowClockTimer = ref(null)       // 1 秒一次的本地时钟刷新定时器
const workflowNow = ref(Date.now())        // 本地时间戳，用于实时刷新“当前节点停留时长”

/** 合同状态对应的中文标签 */
const statusLabel = computed(() => {
  const map = {
    DRAFT: '草稿',
    PENDING_SIGN: '待签署',
    TENANT_SIGNED: '租客已签',
    LANDLORD_SIGNED: '房东已签',
    FULLY_SIGNED: '生效',
    CANCELLED: '已取消'
  }
  return map[contract.value?.status] || contract.value?.status || '-'
})

/** 合同状态对应的 Element Plus Tag 类型 */
const statusType = computed(() => {
  const map = {
    DRAFT: 'info',
    PENDING_SIGN: 'warning',
    TENANT_SIGNED: 'warning',
    LANDLORD_SIGNED: 'warning',
    FULLY_SIGNED: 'success',
    CANCELLED: 'danger'
  }
  return map[contract.value?.status] || 'info'
})

/**
 * 计算是否显示"签署合同"按钮
 * 条件：合同处于待签署状态，且当前用户尚未签署
 */
const showSignBtn = computed(() => {
  if (!contract.value) return false
  const status = contract.value.status
  if (!['DRAFT', 'PENDING_SIGN', 'TENANT_SIGNED', 'LANDLORD_SIGNED'].includes(status)) return false
  if (role === 'LANDLORD' && !contract.value.landlordSigned) return true
  if (role === 'TENANT' && !contract.value.tenantSigned) return true
  return false
})

/** 计算是否显示"取消合同"按钮（双方完成签署前可取消） */
const showCancelBtn = computed(() => {
  return ['DRAFT', 'PENDING_SIGN', 'TENANT_SIGNED', 'LANDLORD_SIGNED'].includes(contract.value?.status)
})

/** 房东需要等待租客先签署时显示醒目提示 */
const landlordNeedWaitHint = computed(() => {
  if (!contract.value || role !== 'LANDLORD') return false
  if (contract.value.landlordSigned) return false
  return !contract.value.tenantSigned && ['DRAFT', 'PENDING_SIGN'].includes(contract.value.status)
})

/**
 * 从合同正文提取“主要条款”用于前端列表展示
 * 处理策略：
 * 1) 仅保留编号条款（如 3.1 / 10.2），过滤标题、签字行、空行；
 * 2) 去掉条款编号前缀，只展示自然语言内容，提升阅读体验；
 * 3) 保留更多条款（最多 12 条），让合同详情更接近真实租赁合同的展示密度。
 */
const clauseList = computed(() => {
  const raw = contract.value?.content || contract.value?.clauses || contract.value?.terms || ''
  return raw
    .split('\n')
    .map(i => i.trim())
    // 仅提取编号条款行，兼容 3、3.1、3.1.1 等格式
    .filter(i => /^\d+(?:\.\d+)*\s*/.test(i))
    .map(i => i.replace(/^\d+(?:\.\d+)*\s*/, ''))
    .filter(i => i && !i.includes('签字：'))
    .slice(0, 12)
})

/**
 * 将后端流程节点转换为前端渲染结构：
 * - shortDesc 用于 Step 的简短摘要；
 * - stayText 支持“当前节点实时增长”展示；
 * - 处理人优先显示姓名，兜底显示 assigneeId。
 */
const workflowNodes = computed(() => {
  const nodes = workflowMonitor.value?.nodes
  if (!Array.isArray(nodes)) return []
  return nodes.map((node) => {
    let liveDuration = null
    if (node.status === 'ACTIVE' && node.enterTime) {
      liveDuration = Math.max(0, workflowNow.value - new Date(node.enterTime).getTime())
    } else if (node.status === 'COMPLETED') {
      liveDuration = node.stayDurationMs
    }
    const handler = node.assigneeName || node.assigneeId || '待分配'
    return {
      ...node,
      shortDesc: `${workflowStatusLabel(node.status)}｜${handler}`,
      stayText: formatDuration(liveDuration)
    }
  })
})

/** 计算 el-steps 的 active 下标（当前处理中的节点序号） */
const workflowActiveIndex = computed(() => {
  const nodes = workflowNodes.value
  if (!nodes.length) return 0
  const activeIndex = nodes.findIndex(i => i.status === 'ACTIVE')
  if (activeIndex >= 0) return activeIndex
  // 流程结束时将 active 设为“最后一步 + 1”，让步骤条完整高亮
  return nodes.every(i => i.status === 'COMPLETED') ? nodes.length : 0
})

onMounted(async () => {
  loading.value = true
  try {
    // 并发加载合同详情和风险检测结果，任一失败不影响另一个
    const [contractRes, risksRes] = await Promise.allSettled([
      getContractDetail(route.params.id),
      getContractRisks(route.params.id)
    ])
    if (contractRes.status === 'fulfilled') contract.value = contractRes.value
    if (risksRes.status === 'fulfilled') {
      risks.value = Array.isArray(risksRes.value) ? risksRes.value : []
    }
    if (!risks.value.length && contract.value?.riskItems) {
      try {
        const parsed = typeof contract.value.riskItems === 'string'
          ? JSON.parse(contract.value.riskItems)
          : contract.value.riskItems
        risks.value = Array.isArray(parsed) ? parsed : []
      } catch (e) {
        console.warn('合同风险数据解析失败：', e)
        risks.value = []
      }
    }
  } catch (e) {
    ElMessage.error('加载合同详情失败')
  } finally {
    loading.value = false
  }
})

/**
 * 监听弹窗开关：
 * - 打开时立即拉取一次监控数据，并启动自动刷新与本地时钟；
 * - 关闭时清理定时器，避免页面后台继续请求。
 */
watch(workflowDialogVisible, async (visible) => {
  if (visible) {
    await loadWorkflowMonitor(true)
    startWorkflowTimers()
  } else {
    stopWorkflowTimers()
  }
})

onBeforeUnmount(() => {
  stopWorkflowTimers()
})

/** 打开流程图监控弹窗 */
function openWorkflowDialog() {
  workflowDialogVisible.value = true
}

/**
 * 拉取流程图监控数据。
 * @param {boolean} forceTip 是否在失败时显示提示（手动刷新/首次打开时为 true）
 */
async function loadWorkflowMonitor(forceTip = false) {
  if (!route.params.id) return
  workflowLoading.value = true
  try {
    const res = await getContractWorkflowMonitor(route.params.id)
    workflowMonitor.value = res
    // 流程已结束时停止自动轮询与秒级计时，避免无意义刷新。
    if (res?.finished) {
      stopWorkflowTimers()
    }
  } catch (e) {
    if (forceTip) {
      ElMessage.error(e.message || '流程图监控数据加载失败')
    }
  } finally {
    workflowLoading.value = false
  }
}

/** 启动流程图监控相关定时器 */
function startWorkflowTimers() {
  stopWorkflowTimers()
  workflowRefreshTimer.value = window.setInterval(() => {
    loadWorkflowMonitor(false)
  }, 10000)
  workflowClockTimer.value = window.setInterval(() => {
    workflowNow.value = Date.now()
  }, 1000)
}

/** 清理流程图监控相关定时器 */
function stopWorkflowTimers() {
  if (workflowRefreshTimer.value) {
    clearInterval(workflowRefreshTimer.value)
    workflowRefreshTimer.value = null
  }
  if (workflowClockTimer.value) {
    clearInterval(workflowClockTimer.value)
    workflowClockTimer.value = null
  }
}

/**
 * 当前用户签署合同
 * 签署成功后重新加载合同数据（更新签署状态和合同状态）
 */
async function handleSign() {
  if (!role) {
    ElMessage.error('无法确定当前角色，请重新登录后再试')
    return
  }
  if (landlordNeedWaitHint.value) {
    ElMessage.warning('租客尚未签署，暂时无法生成房东签署任务，请等待租客先签署。')
    return
  }
  actioning.value = true
  try {
    await signContract(route.params.id, role)
    ElMessage.success('合同签署成功')
    // 重新拉取合同数据以更新双方签署状态
    const res = await getContractDetail(route.params.id)
    contract.value = res
  } catch (e) {
    if (role === 'LANDLORD' && String(e.message || '').includes('未找到合同签署任务')) {
      ElMessage.error('未找到房东签署任务：请先等待租客完成签署后再操作。')
      return
    }
    ElMessage.error(e.message || '签署失败')
  } finally {
    actioning.value = false
  }
}

/**
 * 取消合同
 * 取消成功后本地更新合同状态，避免重新请求
 */
async function handleCancel() {
  actioning.value = true
  try {
    await cancelContract(route.params.id)
    ElMessage.success('合同已取消')
    contract.value.status = 'CANCELLED'  // 本地更新状态
    cancelDialogVisible.value = false
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    actioning.value = false
  }
}

/** 流程节点状态转中文 */
function workflowStatusLabel(status) {
  const map = {
    COMPLETED: '已完成',
    ACTIVE: '处理中',
    PENDING: '未开始'
  }
  return map[status] || status || '-'
}

/** 流程节点状态转步骤条状态 */
function workflowStepStatus(status) {
  if (status === 'COMPLETED') return 'finish'
  if (status === 'ACTIVE') return 'process'
  return 'wait'
}

/** 流程节点状态转标签颜色 */
function workflowTagType(status) {
  if (status === 'COMPLETED') return 'success'
  if (status === 'ACTIVE') return 'warning'
  return 'info'
}

/**
 * 毫秒时长转可读文本（如：2小时15分钟10秒）。
 * 用于展示“节点停留时长”，让流程阻塞点一眼可见。
 */
function formatDuration(ms) {
  if (!Number.isFinite(ms) || ms < 0) return '-'
  const totalSeconds = Math.floor(ms / 1000)
  const days = Math.floor(totalSeconds / 86400)
  const hours = Math.floor((totalSeconds % 86400) / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  const parts = []
  if (days > 0) parts.push(`${days}天`)
  if (hours > 0 || days > 0) parts.push(`${hours}小时`)
  if (minutes > 0 || hours > 0 || days > 0) parts.push(`${minutes}分钟`)
  parts.push(`${seconds}秒`)
  return parts.join('')
}

/** 格式化日期为本地化中文短日期 */
function formatDate(date, withTime = false) {
  if (!date) return '-'
  const val = new Date(date)
  if (Number.isNaN(val.getTime())) return '-'
  const dateText = val.toLocaleDateString('zh-CN')
  const timeText = val.toLocaleTimeString('zh-CN', { hour12: false })
  return withTime ? `${dateText} ${timeText}` : dateText
}

/** 房东类型枚举转中文 */
function ownerTypeLabel(type) {
  const map = { OWNER: '一手房东', SUBLEASE: '二手房东', AGENT: '中介' }
  return map[type] || '-'
}

/** 下载合同 PDF */
async function handleDownloadPdf() {
  try {
    const blob = await downloadContractPdf(route.params.id)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `contract-${route.params.id}.pdf`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(url)
    ElMessage.success('PDF 下载已开始')
  } catch (e) {
    ElMessage.error(e.message || '下载失败')
  }
}
</script>

<style scoped>
.contract-detail-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
}

.page-content {
  flex: 1;
  padding: 32px 20px;
}

.page-inner {
  max-width: 1080px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.back-btn {
  margin-right: 6px;
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  color: #1f2937;
  flex: 1;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.contract-card {
  border-radius: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.card-main-title {
  text-align: center;
  font-size: 32px;
  font-weight: 700;
  color: #111827;
}

.parties-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.party-card {
  flex: 1;
  padding: 18px;
  border-radius: 12px;
  background: #f9fafc;
}

.party-card.landlord {
  background: #f4f5fb;
}

.party-card.tenant {
  background: #eef9f2;
}

.party-card h4 {
  font-size: 18px;
  color: #059669;
  margin-bottom: 14px;
}

.party-row {
  display: flex;
  justify-content: space-between;
  color: #4b5563;
  font-size: 14px;
  padding: 4px 0;
}

.party-row strong {
  color: #111827;
}

.section-block {
  background: #f8fafc;
  border-radius: 12px;
  padding: 18px;
  margin: 14px 0;
}

.section-block h3 {
  margin: 0 0 12px;
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
}

.section-actions {
  margin-top: 10px;
}

.rent-highlight {
  color: #ef4444;
  font-weight: 700;
  font-size: 18px;
}

.clause-list {
  margin: 0;
  padding-left: 20px;
  color: #4b5563;
  line-height: 1.8;
}

.signature-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-top: 12px;
}

.signature-card {
  border: 1px solid #d1fae5;
  border-radius: 12px;
  text-align: center;
  padding: 16px;
  background: #fff;
}

.sig-title {
  font-size: 14px;
  color: #6b7280;
}

.sig-name {
  margin-top: 8px;
  font-size: 24px;
  line-height: 1.1;
  color: #10b981;
  font-weight: 700;
}

.sig-time {
  margin-top: 8px;
  font-size: 13px;
  color: #6b7280;
}

.action-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 8px;
  padding: 8px 0 20px;
}

.landlord-wait-alert {
  border: 2px solid #f59e0b;
}

.risk-card {
  border-radius: 16px;
}

.risk-title {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
}

.loading-wrap {
  max-width: 1080px;
  margin: 40px auto;
  padding: 0 20px;
  width: 100%;
}

.workflow-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.workflow-alert {
  margin-bottom: 14px;
}

.workflow-steps {
  margin: 10px 0 18px;
}

.workflow-node-list {
  max-height: 320px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 10px 12px;
  background: #fafafa;
}

.workflow-node-item {
  padding: 10px 0;
  border-bottom: 1px dashed #e5e7eb;
}

.workflow-node-item:last-child {
  border-bottom: none;
}

.node-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #111827;
}

.node-meta {
  margin-top: 6px;
  color: #4b5563;
  font-size: 13px;
}

@media (max-width: 900px) {
  .page-title {
    font-size: 28px;
  }

  .parties-grid,
  .signature-grid {
    grid-template-columns: 1fr;
  }
}
</style>
