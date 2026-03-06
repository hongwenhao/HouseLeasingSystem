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
                :src="(order.house.images && order.house.images[0]) || placeholder"
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
                {{ formatDateTime(order.appointmentDate) }}
              </el-descriptions-item>
              <el-descriptions-item label="租客" v-if="order.tenant">
                {{ order.tenant.username || order.tenantId }}
              </el-descriptions-item>
              <el-descriptions-item label="房东" v-if="order.landlord">
                {{ order.landlord.username || order.landlordId }}
              </el-descriptions-item>
              <el-descriptions-item label="留言" v-if="order.message">
                {{ order.message }}
              </el-descriptions-item>
              <el-descriptions-item label="创建时间">
                {{ formatDateTime(order.createdAt) }}
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
              <template v-if="role === 'TENANT' && order.status === 'PENDING'">
                <el-button type="warning" size="large" :loading="actioning" @click="handleCancel">
                  取消预约
                </el-button>
              </template>
              <template v-if="role === 'LANDLORD' && order.status === 'CONFIRMED'">
                <el-button type="primary" size="large" @click="handleCreateContract">
                  <el-icon><Document /></el-icon> 生成合同
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

    <Footer />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import NavBar from '../components/NavBar.vue'
import Footer from '../components/Footer.vue'
import { getOrderDetail, confirmOrder, rejectOrder, cancelOrder } from '../api/order.js'
import { createContract } from '../api/contract.js'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const actioning = ref(false)
const order = ref(null)
const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const placeholder = 'https://via.placeholder.com/400x300/409EFF/ffffff?text=房屋图片'
const role = localStorage.getItem('role') || ''

const statusLabel = computed(() => {
  const map = { PENDING: '待确认', CONFIRMED: '已确认', REJECTED: '已拒绝', CANCELLED: '已取消', COMPLETED: '已完成' }
  return map[order.value?.status] || order.value?.status || '-'
})

const statusType = computed(() => {
  const map = { PENDING: 'warning', CONFIRMED: 'success', REJECTED: 'danger', CANCELLED: 'info', COMPLETED: 'primary' }
  return map[order.value?.status] || 'info'
})

const showActions = computed(() => {
  if (!order.value) return false
  if (role === 'LANDLORD' && (order.value.status === 'PENDING' || order.value.status === 'CONFIRMED')) return true
  if (role === 'TENANT' && order.value.status === 'PENDING') return true
  return false
})

const timelineEvents = computed(() => {
  if (!order.value) return []
  const events = [
    { label: '提交预约申请', time: formatDateTime(order.value.createdAt), type: 'primary' }
  ]
  if (order.value.status === 'CONFIRMED') {
    events.push({ label: '房东确认预约', time: formatDateTime(order.value.updatedAt), type: 'success' })
  } else if (order.value.status === 'REJECTED') {
    events.push({ label: '房东拒绝预约', time: formatDateTime(order.value.updatedAt), type: 'danger' })
  } else if (order.value.status === 'CANCELLED') {
    events.push({ label: '预约已取消', time: formatDateTime(order.value.updatedAt), type: 'info' })
  } else if (order.value.status === 'COMPLETED') {
    events.push({ label: '预约完成', time: formatDateTime(order.value.updatedAt), type: 'success' })
  }
  return events
})

onMounted(async () => {
  loading.value = true
  try {
    const res = await getOrderDetail(route.params.id)
    order.value = res
  } catch (e) {
    ElMessage.error('加载订单详情失败')
  } finally {
    loading.value = false
  }
})

async function handleConfirm() {
  actioning.value = true
  try {
    await confirmOrder(route.params.id)
    ElMessage.success('已确认预约')
    order.value.status = 'CONFIRMED'
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    actioning.value = false
  }
}

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

async function handleCreateContract() {
  try {
    const res = await createContract({ orderId: route.params.id })
    ElMessage.success('合同已生成')
    if (res && res.id) {
      router.push(`/contracts/${res.id}`)
    }
  } catch (e) {
    ElMessage.error(e.message || '生成合同失败')
  }
}

function formatDateTime(date) {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN', { hour12: false })
}
</script>

<style scoped>
.order-detail-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.page-content {
  flex: 1;
  padding: 24px 20px;
}

.page-inner {
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
}

.detail-grid {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.status-card {
  border-radius: 12px;
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
  border-radius: 12px;
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
  border-radius: 12px;
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
