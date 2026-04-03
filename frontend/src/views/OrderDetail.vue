<template>
  <div class="order-detail-page">
    <NavBar />
    <div class="page-content" v-if="order">
      <div class="page-inner">
        <div class="page-header">
          <el-button @click="$router.back()" text>
            <el-icon><ArrowLeft /></el-icon> 返回
          </el-button>
          <h2 class="page-title">预约详情</h2>
        </div>

        <div class="detail-grid">
          <!-- Status Card -->
          <el-card class="status-card">
            <div class="status-display">
              <el-tag :type="statusType" size="large" effect="dark">
                {{ statusLabel }}
              </el-tag>
              <p class="order-id">订单编号：{{ order.orderNo || order.id }}</p>
            </div>

            <!-- Timeline -->
            <el-timeline class="order-timeline">
              <el-timeline-item
                v-for="event in timelineEvents"
                :key="event.label"
                :type="event.type"
                :timestamp="event.time"
              >
                {{ event.label }}
              </el-timeline-item>
            </el-timeline>
          </el-card>

          <!-- House Info Card -->
          <el-card class="info-card">
            <template #header>房源信息</template>
            <div class="house-summary" v-if="order.house">
              <img
                :src="order.house.coverImage || placeholder"
                class="house-thumb"
                :alt="order.house.title"
              />
              <div class="house-summary-info">
                <h3>{{ order.house.title }}</h3>
                <p>{{ order.house.city }} {{ order.house.district }}</p>
                <p class="house-price">¥{{ order.house.price }}/月</p>
              </div>
            </div>
            <p v-else class="no-info">房源编号：{{ order.houseId }}</p>
          </el-card>

          <!-- Parties Info Card -->
          <el-card class="info-card">
            <template #header>预约信息</template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="预约看房时间">
                {{ formatDateTime(order.appointmentTime) }}
              </el-descriptions-item>
              <el-descriptions-item label="租赁意向期限">
                {{ formatDate(order.startDate) }} 至 {{ formatDate(order.endDate) }}
              </el-descriptions-item>
              <el-descriptions-item label="押金金额">
                {{ formatMoney(order.deposit) }}
              </el-descriptions-item>
              <el-descriptions-item label="租客" v-if="order.tenant">
                {{ order.tenant.username || order.tenantId }}
              </el-descriptions-item>
              <el-descriptions-item label="房东" v-if="order.landlord">
                {{ order.landlord.username || order.landlordId }}
              </el-descriptions-item>
              <el-descriptions-item label="留言" v-if="order.remark">
                {{ order.remark }}
              </el-descriptions-item>
              <el-descriptions-item label="创建时间">
                {{ formatDateTime(order.createTime) }}
              </el-descriptions-item>
              <el-descriptions-item label="拒绝原因" v-if="order.rejectReason">
                {{ order.rejectReason }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- Actions Card -->
          <el-card class="actions-card" v-if="showActions">
            <template #header>操作</template>
            <div class="action-buttons">
              <template v-if="role === 'LANDLORD' && order.status === 'PENDING'">
                <el-button type="success" size="large" :loading="actioning" @click="handleConfirm">
                  <el-icon><Check /></el-icon> 确认预约
                </el-button>
                <el-button type="danger" size="large" @click="rejectDialogVisible = true">
                  <el-icon><Close /></el-icon> 拒绝预约
                </el-button>
              </template>
              <template v-if="role === 'TENANT' && (order.status === 'PENDING' || order.status === 'APPROVED')">
                <el-button type="warning" size="large" :loading="actioning" @click="handleCancel">
                  取消预约
                </el-button>
              </template>
              <template v-if="role === 'LANDLORD' && order.status === 'APPROVED'">
                <el-button type="primary" size="large" @click="openCreateContractDialog">
                  <el-icon><Document /></el-icon> 生成合同
                </el-button>
              </template>
              <template v-if="role === 'LANDLORD' && (order.status === 'PENDING' || order.status === 'APPROVED')">
                <el-button type="warning" size="large" :loading="actioning" @click="handleCancel">
                  取消订单
                </el-button>
              </template>
            </div>
          </el-card>
        </div>
      </div>
    </div>
    <div v-else-if="loading" class="loading-wrap">
      <el-skeleton :rows="8" animated />
    </div>
    <el-empty v-else description="订单不存在" />

    <!-- Reject Dialog -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝预约" width="400px">
      <el-form>
        <el-form-item label="拒绝原因">
          <el-input
            v-model="rejectReason"
            type="textarea"
            :rows="3"
            placeholder="请输入拒绝原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="actioning" @click="handleReject">确认拒绝</el-button>
      </template>
    </el-dialog>

    <!-- Create Contract Dialog -->
    <el-dialog v-model="createContractDialogVisible" title="生成合同" width="460px">
      <el-form :model="contractForm" label-width="110px">
        <el-form-item label="租赁开始日期" required>
          <el-date-picker
            v-model="contractForm.startDate"
            type="date"
            placeholder="请选择开始日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="租赁结束日期" required>
          <el-date-picker
            v-model="contractForm.endDate"
            type="date"
            placeholder="请选择结束日期"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createContractDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creatingContract" @click="handleCreateContract">
          确认生成
        </el-button>
      </template>
    </el-dialog>

    <Footer />
  </div>
</template>

<script setup>
// 说明：预约订单详情页逻辑，展示订单状态时间线，并根据角色提供确认/拒绝/取消预约等操作
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import NavBar from '../components/NavBar.vue'
import Footer from '../components/Footer.vue'
import { getOrderDetail, confirmOrder, rejectOrder, cancelOrder } from '../api/order.js'
import { createContract } from '../api/contract.js'

const route = useRoute()
const router = useRouter()
const loading = ref(false)           // 页面加载状态
const actioning = ref(false)         // 操作按钮 loading（确认/拒绝/取消）
const order = ref(null)              // 订单详情数据
const rejectDialogVisible = ref(false) // 拒绝对话框显隐
const rejectReason = ref('')         // 拒绝原因
const createContractDialogVisible = ref(false) // 生成合同对话框显隐
const creatingContract = ref(false)            // 生成合同按钮 loading
const contractForm = ref({
  startDate: null, // 合同租赁开始日期（房东填写）
  endDate: null    // 合同租赁结束日期（房东填写）
})
const placeholder = 'https://via.placeholder.com/400x300/409EFF/ffffff?text=房屋图片'
const role = localStorage.getItem('role') || ''  // 当前用户角色（从 localStorage 读取）

/** 订单状态对应的中文标签 */
const statusLabel = computed(() => {
  const map = { PENDING: '待房东确认', APPROVED: '房东已确认', REJECTED: '房东已拒绝', CANCELLED: '订单已取消', COMPLETED: '订单已完成' }
  return map[order.value?.status] || order.value?.status || '-'
})

/** 订单状态对应的 Element Plus Tag 类型（颜色） */
const statusType = computed(() => {
  const map = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info', COMPLETED: 'primary' }
  return map[order.value?.status] || 'info'
})

/**
             * 计算是否显示操作按钮区域
             * 房东可操作：
             * - PENDING：确认/拒绝/取消
             * - APPROVED：生成合同/取消
             * 租客可操作：
             * - PENDING：取消预约
             * - APPROVED：取消预约（未签约前允许双方撤销预约）
             */
const showActions = computed(() => {
  if (!order.value) return false
  if (role === 'LANDLORD' && (order.value.status === 'PENDING' || order.value.status === 'APPROVED')) return true
  if (role === 'TENANT' && (order.value.status === 'PENDING' || order.value.status === 'APPROVED')) return true
  return false
})

/**
 * 计算订单状态时间线事件列表
 * 基于订单状态动态生成已发生的节点（创建、确认/拒绝/取消/完成）
 */
const timelineEvents = computed(() => {
  if (!order.value) return []
  const events = [
    { label: '提交预约申请', time: formatDateTime(order.value.createTime), type: 'primary' }
  ]
  if (order.value.status === 'APPROVED') {
    events.push({ label: '房东确认预约', time: formatDateTime(order.value.updateTime), type: 'success' })
  } else if (order.value.status === 'REJECTED') {
    events.push({ label: '房东拒绝预约', time: formatDateTime(order.value.updateTime), type: 'danger' })
  } else if (order.value.status === 'CANCELLED') {
    events.push({ label: '预约已取消', time: formatDateTime(order.value.updateTime), type: 'info' })
  } else if (order.value.status === 'COMPLETED') {
    events.push({ label: '预约完成', time: formatDateTime(order.value.updateTime), type: 'success' })
  }
  return events
})

onMounted(async () => {
  loading.value = true
  try {
    const res = await getOrderDetail(route.params.id)
    order.value = res
    // 若预约中已存在租赁起止日期，默认回填到生成合同弹窗，方便房东确认或调整
    contractForm.value.startDate = res?.startDate ? new Date(res.startDate) : null
    contractForm.value.endDate = res?.endDate ? new Date(res.endDate) : null
  } catch (e) {
    ElMessage.error('加载订单详情失败')
  } finally {
    loading.value = false
  }
})

/** 房东确认预约 */
async function handleConfirm() {
  actioning.value = true
  try {
    await confirmOrder(route.params.id)
    ElMessage.success('已确认预约')
    order.value.status = 'APPROVED'  // 本地更新状态，无需重新请求
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    actioning.value = false
  }
}

/** 房东拒绝预约（附带拒绝原因） */
async function handleReject() {
  actioning.value = true
  try {
    await rejectOrder(route.params.id, { reason: rejectReason.value })
    ElMessage.success('已拒绝预约')
    order.value.status = 'REJECTED'
    order.value.rejectReason = rejectReason.value
    rejectDialogVisible.value = false
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    actioning.value = false
  }
}

/** 租客取消预约 */
async function handleCancel() {
  actioning.value = true
  try {
    await cancelOrder(route.params.id)
    ElMessage.success('已取消预约')
    order.value.status = 'CANCELLED'
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    actioning.value = false
  }
}

/**
 * 房东根据已确认订单生成合同
 * 成功后跳转到新创建的合同详情页
 */
async function handleCreateContract() {
  if (!contractForm.value.startDate || !contractForm.value.endDate) {
    ElMessage.warning('请填写完整的租赁起止日期')
    return
  }
  if (contractForm.value.endDate < contractForm.value.startDate) {
    ElMessage.warning('租赁结束日期不能早于开始日期')
    return
  }
  creatingContract.value = true
  try {
    const res = await createContract({
      orderId: route.params.id,
      startDate: formatDateForApi(contractForm.value.startDate),
      endDate: formatDateForApi(contractForm.value.endDate)
    })
    ElMessage.success('合同已生成')
    createContractDialogVisible.value = false
    if (res && res.id) {
      router.push(`/contracts/${res.id}`)
    }
  } catch (e) {
    ElMessage.error(e.message || '生成合同失败')
  } finally {
    creatingContract.value = false
  }
}

/** 格式化日期时间为本地化中文完整时间字符串（24小时制） */
function formatDateTime(date) {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN', { hour12: false })
}

/** 格式化日期为 yyyy-MM-dd，供后端 LocalDate 字段接收 */
function formatDateForApi(date) {
  if (!date) return null
  const d = new Date(date)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** 格式化日期为本地化中文短日期 */
function formatDate(date) {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('zh-CN')
}

/** 格式化金额展示 */
function formatMoney(val) {
  if (val === null || val === undefined || val === '') return '-'
  return `¥${val}`
}

/** 打开生成合同对话框（房东在此填写正式租期） */
function openCreateContractDialog() {
  createContractDialogVisible.value = true
}
</script>

<style scoped>
.order-detail-page {
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
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
}

.detail-grid {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.status-card {
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}

.status-display {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.order-id {
  font-size: 13px;
  color: #909399;
}

.order-timeline {
  padding-top: 8px;
}

.info-card {
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}

.house-summary {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.house-thumb {
  width: 120px;
  height: 80px;
  object-fit: cover;
  border-radius: 6px;
  flex-shrink: 0;
}

.house-summary-info h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
  color: #303133;
}

.house-summary-info p {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.house-price {
  font-size: 18px !important;
  font-weight: 700 !important;
  color: #f56c6c !important;
}

.no-info {
  color: #909399;
  font-size: 14px;
}

.actions-card {
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}

.action-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.loading-wrap {
  max-width: 900px;
  margin: 40px auto;
  padding: 0 20px;
  width: 100%;
}
</style>
