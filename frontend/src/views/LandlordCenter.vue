<template>
  <div class="landlord-center-page">
    <NavBar />
    <div class="page-content">
      <div class="page-inner">
        <h2 class="page-title">房东中心</h2>
        <el-tabs v-model="activeTab" class="center-tabs">
          <!-- My Houses Tab -->
          <el-tab-pane label="我的房源" name="houses">
            <div class="tab-toolbar">
              <el-button type="primary" @click="$router.push('/publish-house')">
                <el-icon><Plus /></el-icon> 发布新房源
              </el-button>
            </div>
            <div v-if="housesLoading">
              <el-skeleton :rows="4" animated />
            </div>
            <div v-else-if="myHouses.length > 0">
              <div
                v-for="house in myHouses"
                :key="house.id"
                class="house-item"
              >
                <img
                  :src="getHouseCover(house)"
                  class="house-thumb"
                  :alt="house.title"
                />
                <div class="house-item-info">
                  <div class="house-item-header">
                    <span class="house-item-title">{{ house.title }}</span>
                    <el-tag :type="houseStatusType(house.status)" size="small">
                      {{ houseStatusLabel(house.status) }}
                    </el-tag>
                  </div>
                  <div class="house-item-meta">
                    <span>{{ house.city }} {{ house.district }}</span>
                    <span>¥{{ house.price }}/月</span>
                    <span>{{ house.area }}㎡</span>
                  </div>
                </div>
                <div class="house-item-actions">
                  <el-button size="small" type="primary" @click="$router.push(`/publish-house/${house.id}`)">编辑</el-button>
                  <el-button size="small" type="danger" @click="deleteMyHouse(house.id)">删除</el-button>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无房源，快去发布吧" />
          </el-tab-pane>

          <!-- Orders Tab -->
          <el-tab-pane label="预约管理" name="orders">
            <div v-if="ordersLoading">
              <el-skeleton :rows="4" animated />
            </div>
            <div v-else-if="landlordOrders.length > 0">
              <div
                v-for="order in landlordOrders"
                :key="order.id"
                class="order-item"
              >
                <div class="order-info">
                  <div>
                    <span class="order-title">{{ order.house?.title || order.houseTitle || `房源#${order.houseId}` }}</span>
                    <span class="tenant-name"> — 租客：{{ order.tenant?.realName || order.tenant?.username || order.tenantName || order.tenantId }}</span>
                  </div>
                  <el-tag :type="orderStatusType(order.status)" size="small">
                    {{ orderStatusLabel(order.status) }}
                  </el-tag>
                </div>
                <div class="order-meta">
                  <span>预约时间：{{ formatDateTime(order.appointmentTime) }}</span>
                  <span>押金：¥{{ order.deposit ?? '-' }}</span>
                  <span v-if="order.remark || order.message">留言：{{ order.remark || order.message }}</span>
                </div>
                <div class="order-actions" v-if="order.status === 'PENDING'">
                  <el-button size="small" type="success" @click="handleConfirmOrder(order.id)">确认预约</el-button>
                  <el-button size="small" type="danger" @click="openRejectDialog(order)">拒绝</el-button>
                </div>
                <div class="order-actions" v-else>
                  <el-button size="small" @click="$router.push(`/orders/${order.id}`)">查看详情</el-button>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无预约记录" />
          </el-tab-pane>

          <!-- Contracts Tab -->
          <el-tab-pane label="合同管理" name="contracts">
            <div v-if="contractsLoading">
              <el-skeleton :rows="4" animated />
            </div>
            <div v-else-if="contracts.length > 0">
              <div
                v-for="contract in contracts"
                :key="contract.id"
                class="contract-item"
              >
                <div class="contract-info">
                  <span class="contract-no">合同编号：{{ contract.contractNo || contract.id }}</span>
                  <el-tag :type="contractStatusType(contract.status)" size="small">
                    {{ contractStatusLabel(contract.status) }}
                  </el-tag>
                </div>
                <div class="contract-meta">
                  <span>租客：{{ contract.tenantName || contract.tenantId }}</span>
                  <span>租期：{{ formatDate(contract.startDate) }} 至 {{ formatDate(contract.endDate) }}</span>
                  <span>月租：¥{{ contract.monthlyRent ?? contract.rent }}</span>
                </div>
                <el-button size="small" @click="$router.push(`/contracts/${contract.id}`)">查看合同</el-button>
              </div>
            </div>
            <el-empty v-else description="暂无合同记录" />
          </el-tab-pane>

          <!-- Stats Tab -->
          <el-tab-pane label="收益统计" name="stats">
            <div class="stats-cards">
              <el-card class="stat-card">
                <div class="stat-num">¥{{ stats.totalIncome || 0 }}</div>
                <div class="stat-label">累计收益</div>
              </el-card>
              <el-card class="stat-card">
                <div class="stat-num">{{ stats.activeRentals || 0 }}</div>
                <div class="stat-label">在租房源</div>
              </el-card>
              <el-card class="stat-card">
                <div class="stat-num">¥{{ stats.avgPrice || 0 }}</div>
                <div class="stat-label">平均租金</div>
              </el-card>
              <el-card class="stat-card">
                <div class="stat-num">{{ stats.totalHouses || 0 }}</div>
                <div class="stat-label">总房源数</div>
              </el-card>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

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
        <el-button type="danger" :loading="rejecting" @click="submitReject">确认拒绝</el-button>
      </template>
    </el-dialog>

    <Footer />
  </div>
</template>

<script setup>
// 说明：房东中心页逻辑，管理房东的房源列表、预约订单管理、合同管理和收益统计
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import NavBar from '../components/NavBar.vue'
import Footer from '../components/Footer.vue'
import { getMyHouses, deleteHouse } from '../api/house.js'
import { getLandlordOrders, confirmOrder, rejectOrder } from '../api/order.js'
import { getMyContracts } from '../api/contract.js'
import { normalizeHouseImages } from '../utils/houseImages.js'

const activeTab = ref('houses')        // 当前激活 tab
const route = useRoute()               // 当前路由对象，用于读取 ?tab=xxx 参数
const housesLoading = ref(false)       // 我的房源加载状态
const ordersLoading = ref(false)       // 预约订单加载状态
const contractsLoading = ref(false)    // 合同列表加载状态
const myHouses = ref([])               // 当前房东发布的所有房源
const landlordOrders = ref([])         // 收到的预约订单列表
const contracts = ref([])              // 参与的合同列表
const stats = ref({})                  // 统计数据（累计收益、在租数等）
const rejectDialogVisible = ref(false) // 拒绝预约对话框显隐
const rejectReason = ref('')           // 拒绝原因输入内容
const rejecting = ref(false)           // 拒绝按钮 loading 状态
const currentRejectOrder = ref(null)   // 当前正在被拒绝的订单
const placeholder = 'https://via.placeholder.com/400x300/409EFF/ffffff?text=房屋图片'
// 允许通过 query 切换的标签页白名单（仅接受已有标签，避免无效参数）
const allowedTabs = ['houses', 'orders', 'contracts', 'stats']

onMounted(() => {
  // 房源和合同数据加载完成后再计算统计信息（computeStats 依赖这两项数据）
  Promise.all([loadHouses(), loadContracts()]).then(() => computeStats())
  // 预约订单独立加载（统计信息不依赖订单数据，两者互不阻塞）
  loadOrders()
})

/**
 * 监听 query.tab 变化，支持在房东中心页内重复点击顶部导航后即时切换标签页。
 * 对非白名单参数保持忽略，防止 URL 参数异常导致 UI 状态错误。
 */
watch(
  () => route.query.tab,
  (tab) => {
    const targetTab = typeof tab === 'string' ? tab : ''
    if (allowedTabs.includes(targetTab)) {
      activeTab.value = targetTab
    }
  },
  { immediate: true }
)

/** 加载房东自己发布的房源列表 */
async function loadHouses() {
  housesLoading.value = true
  try {
    const res = await getMyHouses({ page: 1, size: 50 })
    // 后端返回 PageResult 对象，其数据列表字段为 records（非 list）
    myHouses.value = (Array.isArray(res) ? res : (res?.records || []))
      .map(house => ({ ...house, images: normalizeHouseImages(house.images) }))
  } catch (e) { /* ignore */ }
  finally { housesLoading.value = false }
}

/** 加载收到的预约订单列表 */
async function loadOrders() {
  ordersLoading.value = true
  try {
    const res = await getLandlordOrders({ page: 1, size: 50 })
    // 后端返回 PageResult 对象，其数据列表字段为 records（非 list）
    landlordOrders.value = Array.isArray(res) ? res : (res?.records || [])
  } catch (e) { /* ignore */ }
  finally { ordersLoading.value = false }
}

/** 加载参与的合同列表 */
async function loadContracts() {
  contractsLoading.value = true
  try {
    const res = await getMyContracts({ page: 1, size: 50 })
    // 后端返回 PageResult 对象，其数据列表字段为 records（非 list）
    contracts.value = Array.isArray(res) ? res : (res?.records || [])
  } catch (e) { /* ignore */ }
  finally { contractsLoading.value = false }
}

/**
 * 计算房东收益统计数据（前端聚合计算）
 * - totalHouses：总房源数
 * - activeRentals：当前生效中的合同数（在租房源数）
 * - totalIncome：所有在租合同的月租金总和
 * - avgPrice：所有房源月租金的平均值
 */
function computeStats() {
  stats.value = {
    totalHouses: myHouses.value.length,
    activeRentals: contracts.value.filter(c => c.status === 'FULLY_SIGNED').length,
    totalIncome: contracts.value.filter(c => c.status === 'FULLY_SIGNED').reduce((sum, c) => sum + (c.monthlyRent || c.rent || 0), 0),
    avgPrice: myHouses.value.length > 0
      ? Math.round(myHouses.value.reduce((sum, h) => sum + (h.price || 0), 0) / myHouses.value.length)
      : 0
  }
}

/**
 * 删除指定房源（需二次确认弹框）
 * @param {number} id - 房源 ID
 */
async function deleteMyHouse(id) {
  await ElMessageBox.confirm('确定要删除该房源吗？', '删除确认', { type: 'warning' })
  try {
    await deleteHouse(id)
    ElMessage.success('删除成功')
    loadHouses()  // 重新加载房源列表
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

/** 确认预约订单（房东操作） */
async function handleConfirmOrder(id) {
  try {
    await confirmOrder(id)
    ElMessage.success('已确认预约')
    loadOrders()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

/**
 * 打开拒绝预约对话框
 * @param {Object} order - 要拒绝的订单对象
 */
function openRejectDialog(order) {
  currentRejectOrder.value = order
  rejectReason.value = ''
  rejectDialogVisible.value = true
}

/**
 * 获取房源列表卡片封面图：优先取 images 第1张，没有则占位图
 * 该函数兼容 images 是数组、JSON 字符串、单 URL 字符串等历史数据格式。
 */
function getHouseCover(house) {
  const images = normalizeHouseImages(house?.images)
  return images.length > 0 ? images[0] : placeholder
}

/** 提交拒绝预约（附带拒绝原因） */
async function submitReject() {
  rejecting.value = true
  try {
    await rejectOrder(currentRejectOrder.value.id, { reason: rejectReason.value })
    ElMessage.success('已拒绝预约')
    rejectDialogVisible.value = false
    loadOrders()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    rejecting.value = false
  }
}

/** 房源状态枚举转中文 */
function houseStatusLabel(status) {
  const map = { PENDING: '审核中', ONLINE: '已上架', APPROVED: '已上架', REJECTED: '审核拒绝', OFFLINE: '已下架' }
  return map[status] || status
}

/** 房源状态对应 Tag 类型 */
function houseStatusType(status) {
  const map = { PENDING: 'warning', ONLINE: 'success', APPROVED: 'success', REJECTED: 'danger', OFFLINE: 'info' }
  return map[status] || 'info'
}

/** 订单状态枚举转中文 */
function orderStatusLabel(status) {
  const map = { PENDING: '待确认', APPROVED: '已确认', REJECTED: '已拒绝', CANCELLED: '已取消', COMPLETED: '已完成' }
  return map[status] || status
}

/** 订单状态对应 Tag 类型 */
function orderStatusType(status) {
  const map = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info', COMPLETED: 'primary' }
  return map[status] || 'info'
}

/** 合同状态枚举转中文 */
function contractStatusLabel(status) {
  const map = { DRAFT: '草稿', PENDING_SIGN: '待签署', TENANT_SIGNED: '租客已签', LANDLORD_SIGNED: '房东已签', FULLY_SIGNED: '双方已签', CANCELLED: '已取消' }
  return map[status] || status
}

/** 合同状态对应 Tag 类型 */
function contractStatusType(status) {
  const map = { DRAFT: 'info', PENDING_SIGN: 'warning', TENANT_SIGNED: 'warning', LANDLORD_SIGNED: 'warning', FULLY_SIGNED: 'success', CANCELLED: 'danger' }
  return map[status] || 'info'
}

/** 格式化日期为本地化中文短日期 */
function formatDate(date) {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('zh-CN')
}

function formatDateTime(date) {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN', { hour12: false })
}
</script>

<style scoped>
.landlord-center-page {
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
  max-width: 1000px;
  margin: 0 auto;
}

.page-title {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 24px;
  position: relative;
  padding-left: 16px;
}

.page-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 4px;
  border-radius: 2px;
  background: linear-gradient(180deg, #667eea, #764ba2);
}

.center-tabs {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}

.tab-toolbar {
  margin-bottom: 16px;
}

.house-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 16px;
  margin-bottom: 12px;
  background: #fafafa;
  transition: box-shadow 0.3s ease;
}

.house-item:hover {
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}

.house-thumb {
  width: 100px;
  height: 70px;
  object-fit: cover;
  border-radius: 6px;
  flex-shrink: 0;
}

.house-item-info {
  flex: 1;
  min-width: 0;
}

.house-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.house-item-title {
  font-weight: 600;
  color: #303133;
  font-size: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.house-item-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #909399;
}

.house-item-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.order-item, .contract-item {
  background: #f9fafb;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  border: 1px solid #ebeef5;
}

.order-info, .contract-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.order-title, .contract-no {
  font-weight: 600;
  color: #303133;
}

.tenant-name {
  font-size: 13px;
  color: #909399;
}

.order-meta, .contract-meta {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: #909399;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.order-actions {
  display: flex;
  gap: 8px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 20px;
}

.stat-card {
  text-align: center;
  padding: 24px;
  border-radius: 16px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
}

.stat-num {
  font-size: 32px;
  font-weight: 800;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}
</style>
