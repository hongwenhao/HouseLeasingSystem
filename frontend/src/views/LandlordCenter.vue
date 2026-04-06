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
              <el-button type="primary" @click="router.push('/publish-house')">
                <el-icon><Plus /></el-icon> 发布新房源
              </el-button>
              <!-- 我的房源搜索栏：按标题、城市、区县、状态关键字进行前端过滤 -->
              <el-input
                v-model.trim="houseSearchKeyword"
                class="search-input"
                clearable
                placeholder="搜索房源（标题/城市/区县/状态）"
              />
            </div>
            <div v-if="housesLoading">
              <el-skeleton :rows="4" animated />
            </div>
            <!-- 我的房源：采用“表头 + 行内容”结构，明确展示房源信息、上下架状态与操作 -->
            <div v-else-if="filteredMyHouses.length > 0" class="house-table">
              <!-- 表头：新增“房源信息”导航标识，并将“上下架状态”与“操作”并列展示 -->
              <div class="house-table-head">
                <span>房源信息</span>
                <span class="status-col-head">上下架状态</span>
                <span class="action-col-head">操作</span>
              </div>
              <div
                v-for="house in filteredMyHouses"
                :key="house.id"
                class="house-table-row"
              >
                <!-- 第一列：房源信息（缩略图 + 标题 + 基础属性） -->
                <div class="house-info-col">
                  <img
                    :src="getHouseCover(house)"
                    class="house-thumb"
                    :alt="house.title"
                  />
                  <div class="house-item-info">
                    <div class="house-item-header">
                      <span class="house-item-title">{{ house.title }}</span>
                    </div>
                    <div class="house-item-meta">
                      <span>{{ house.city }} {{ house.district }}</span>
                      <span>¥{{ house.price }}/月</span>
                      <span>{{ house.area }}㎡</span>
                    </div>
                  </div>
                </div>
                <!-- 第二列：上下架状态，单独成列与操作列平行对齐 -->
                <div class="house-status-col">
                  <el-tag :type="houseStatusType(house.status)" size="small">
                    {{ houseStatusLabel(house.status) }}
                  </el-tag>
                </div>
                <!-- 第三列：操作按钮 -->
                <div class="house-item-actions">
                  <el-button
                    v-if="canPutHouseOnline(house.status)"
                    size="small"
                    type="success"
                    plain
                    @click="putHouseOnline(house.id)"
                  >
                    上架
                  </el-button>
                  <el-button
                    v-if="canPutHouseOffline(house.status)"
                    size="small"
                    type="warning"
                    plain
                    @click="putHouseOffline(house.id)"
                  >
                    下架
                  </el-button>
                  <el-button size="small" type="primary" @click="router.push(`/publish-house/${house.id}`)">编辑</el-button>
                  <el-button size="small" type="danger" @click="deleteMyHouse(house.id)">删除</el-button>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无房源，快去发布吧" />
          </el-tab-pane>

          <!-- Orders Tab -->
          <el-tab-pane label="预约订单管理" name="orders">
            <div class="table-toolbar">
              <!-- 预约订单搜索栏：按订单号、房源、租客、状态、时间等信息模糊匹配 -->
              <el-input
                v-model.trim="orderSearchKeyword"
                class="search-input"
                clearable
                placeholder="搜索预约订单（订单号/房源/租客/状态/时间）"
              />
            </div>
            <div v-if="ordersLoading">
              <el-skeleton :rows="4" animated />
            </div>
            <div v-else-if="filteredLandlordOrders.length > 0" class="table-card orders-table">
              <div class="table-head">
                <span>预约房源</span>
                <span>租客</span>
                <span>预约时间</span>
                <span>订单状态</span>
                <span>支付状态</span>
                <span class="action-head">操作</span>
              </div>
              <div
                v-for="order in filteredLandlordOrders"
                :key="order.id"
                class="table-row"
              >
                <!-- 预约房源优先展示真实房源标题，兼容不同接口结构 -->
                <span class="title-cell">{{ getOrderHouseTitleWithFallback(order) }}</span>
                <span>{{ order.tenant?.realName || order.tenant?.username || order.tenantName || order.tenantId }}</span>
                <span>{{ formatDateTime(order.appointmentTime) }}</span>
                <span>
                  <el-tag :type="orderStatusType(order.status)" size="small">
                    {{ orderStatusLabel(order.status) }}
                  </el-tag>
                </span>
                <span>
                  <el-tag :type="paymentStatusType(order.paymentStatus)" size="small">
                    {{ paymentStatusLabel(order.paymentStatus) }}
                  </el-tag>
                </span>
                <div class="row-actions">
                  <el-button size="small" @click="router.push(`/orders/${order.id}`)">查看订单</el-button>
                  <!-- 直接平铺可执行操作，不再收纳到“更多操作”下拉，减少点击路径并提升可见性 -->
                  <el-button
                    v-if="order.contractId"
                    size="small"
                    type="primary"
                    plain
                    @click="router.push(`/contracts/${order.contractId}`)"
                  >
                    查看合同
                  </el-button>
                  <el-button
                    v-if="order.status === 'PENDING'"
                    size="small"
                    type="success"
                    plain
                    @click="handleConfirmOrder(order.id)"
                  >
                    确认预约
                  </el-button>
                  <el-button
                    v-if="order.status === 'PENDING'"
                    size="small"
                    type="danger"
                    plain
                    @click="openRejectDialog(order)"
                  >
                    拒绝预约
                  </el-button>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无预约记录" />
          </el-tab-pane>

          <!-- Contracts Tab -->
          <el-tab-pane label="合同管理" name="contracts">
            <div class="table-toolbar">
              <!-- 合同搜索栏：按合同编号、租客、租期、状态做本地筛选 -->
              <el-input
                v-model.trim="contractSearchKeyword"
                class="search-input"
                clearable
                placeholder="搜索合同（合同编号/租客/租期/状态）"
              />
            </div>
            <div v-if="contractsLoading">
              <el-skeleton :rows="4" animated />
            </div>
            <div v-else-if="filteredContracts.length > 0" class="table-card contracts-table">
              <div class="table-head">
                <span>合同编号</span>
                <span>租客</span>
                <span>租期</span>
                <span>月租</span>
                <span>状态</span>
                <span class="action-head">操作</span>
              </div>
              <div
                v-for="contract in filteredContracts"
                :key="contract.id"
                class="table-row"
              >
                <span class="title-cell">{{ contract.contractNo || contract.id }}</span>
                <span>{{ contract.tenantName || contract.tenant?.realName || contract.tenant?.username || contract.tenantId }}</span>
                <span>{{ formatDate(contract.startDate) }} 至 {{ formatDate(contract.endDate) }}</span>
                <span>¥{{ contract.monthlyRent ?? contract.rent }}</span>
                <span>
                  <el-tag :type="contractStatusType(contract.status)" size="small">
                    {{ contractStatusLabel(contract.status) }}
                  </el-tag>
                </span>
                <div class="row-actions">
                  <el-button size="small" @click="router.push(`/contracts/${contract.id}`)">查看合同</el-button>
                  <el-button
                    v-if="contract.orderId"
                    size="small"
                    type="primary"
                    plain
                    @click="router.push(`/orders/${contract.orderId}`)"
                  >
                    查看订单
                  </el-button>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无合同记录" />
          </el-tab-pane>

          <!-- Reviews Tab：按需求放在“合同管理”后面 -->
          <el-tab-pane label="收到的评价" name="reviews">
            <div v-if="reviewsLoading">
              <el-skeleton :rows="4" animated />
            </div>
            <div v-else-if="reviewRecords.length > 0" class="review-list">
              <div v-for="review in reviewRecords" :key="review.id" class="review-item">
                <div class="review-item-head">
                  <h4 class="review-house-title">{{ review.houseTitle || (review.houseId ? `房源#${review.houseId}` : '-') }}</h4>
                  <el-rate :model-value="review.rating || 0" disabled show-score />
                </div>
                <div class="review-item-meta">
                  <span>租客：{{ review.tenantName || (review.tenantId ? `用户#${review.tenantId}` : '-') }}</span>
                  <!-- 优先展示业务订单号 orderNo，历史数据缺失时回退 orderId -->
                  <span>订单编号：{{ review.orderNo || review.orderId || '-' }}</span>
                </div>
                <!-- 评价时间独立成行并左对齐，避免与其它元信息同排时出现错位 -->
                <div class="review-item-time">评价时间：{{ formatDateTime(review.createTime) }}</div>
                <div class="review-item-content">{{ review.content || '（未填写评价内容）' }}</div>
              </div>
            </div>
            <el-empty v-else description="暂无收到的评价" />
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
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import NavBar from '../components/NavBar.vue'
import Footer from '../components/Footer.vue'
import { getMyHouses, deleteHouse, putMyHouseOnline, putMyHouseOffline } from '../api/house.js'
import { getLandlordOrders, confirmOrder, rejectOrder, getLandlordReviewRecords } from '../api/order.js'
import { getMyContracts } from '../api/contract.js'
import { normalizeHouseImages } from '../utils/houseImages.js'

const activeTab = ref('houses')        // 当前激活 tab
const route = useRoute()               // 当前路由对象，用于读取 ?tab=xxx 参数
const router = useRouter()             // 路由实例（用于命令式跳转）
const housesLoading = ref(false)       // 我的房源加载状态
const ordersLoading = ref(false)       // 预约订单加载状态
const contractsLoading = ref(false)    // 合同列表加载状态
const reviewsLoading = ref(false)      // 评价列表加载状态
const myHouses = ref([])               // 当前房东发布的所有房源
const landlordOrders = ref([])         // 收到的预约订单列表
const contracts = ref([])              // 参与的合同列表
const houseSearchKeyword = ref('')     // 我的房源搜索关键字（前端本地过滤，不触发额外请求）
const orderSearchKeyword = ref('')     // 预约订单搜索关键字（前端本地过滤，不触发额外请求）
const contractSearchKeyword = ref('')  // 合同搜索关键字（前端本地过滤，不触发额外请求）
const reviewRecords = ref([])          // 收到的评价列表
const stats = ref({})                  // 统计数据（累计收益、在租数等）
const rejectDialogVisible = ref(false) // 拒绝预约对话框显隐
const rejectReason = ref('')           // 拒绝原因输入内容
const rejecting = ref(false)           // 拒绝按钮 loading 状态
const currentRejectOrder = ref(null)   // 当前正在被拒绝的订单
const placeholder = 'https://via.placeholder.com/400x300/409EFF/ffffff?text=房屋图片'
// 允许通过 query 切换的标签页白名单（仅接受已有标签，避免无效参数）
const allowedTabs = ['houses', 'orders', 'contracts', 'stats', 'reviews']

/**
 * 将房东中心当前激活标签同步到 URL query.tab。
 * 说明：顶部导航栏“预约订单管理/合同管理/收到的评价”激活态依赖 query.tab。
 */
async function syncTabToRouteQuery(tabName) {
  const targetTab = typeof tabName === 'string' ? tabName : ''
  if (!allowedTabs.includes(targetTab)) return
  if (route.query.tab === targetTab) return
  const nextQuery = { ...route.query, tab: targetTab }
  await router.replace({ path: route.path, query: nextQuery })
}

/**
 * 将任意值统一转换为可用于搜索比较的字符串。
 * - null/undefined 转为空串，避免出现 "undefined" 等无意义文本；
 * - 统一转小写，保证搜索不区分大小写；
 * - trim 去除首尾空白，减少用户输入空格造成的误差。
 */
function normalizeSearchText(value) {
  return String(value ?? '').trim().toLowerCase()
}

/**
 * 通用关键字匹配函数：只要候选字段任意一个包含关键字，即视为命中。
 * @param {string} keyword - 用户输入的搜索关键字
 * @param {Array<string|number|null|undefined>} candidates - 待匹配字段集合
 */
function containsKeyword(keyword, candidates) {
  const normalizedKeyword = normalizeSearchText(keyword)
  if (!normalizedKeyword) return true
  return candidates.some(item => normalizeSearchText(item).includes(normalizedKeyword))
}

/**
 * 我的房源搜索结果：
 * 支持标题、城市、区县、价格、面积和状态文本过滤，方便房东快速定位目标房源。
 */
const filteredMyHouses = computed(() => myHouses.value.filter(house => containsKeyword(houseSearchKeyword.value, [
  house.title,
  house.city,
  house.district,
  house.price,
  house.area,
  houseStatusLabel(house.status)
])))

/**
 * 预约订单搜索结果：
 * 支持订单号、房源名、租客名、订单状态、支付状态、预约时间等字段过滤。
 * 说明：
 * - 房东在处理大量预约时，常通过订单号进行工单/聊天核对；
 * - 将 orderNo 纳入本地过滤字段，可减少人工逐条翻找成本。
 */
const filteredLandlordOrders = computed(() => landlordOrders.value.filter(order => containsKeyword(orderSearchKeyword.value, [
  order.orderNo,
  getOrderHouseTitleWithFallback(order),
  order.tenant?.realName,
  order.tenant?.username,
  order.tenantName,
  order.tenantId,
  orderStatusLabel(order.status),
  paymentStatusLabel(order.paymentStatus),
  formatDateTime(order.appointmentTime)
])))

/**
 * 合同搜索结果：
 * 支持合同编号、租客、状态、租期时间等字段过滤，便于查找历史合同。
 */
const filteredContracts = computed(() => contracts.value.filter(contract => containsKeyword(contractSearchKeyword.value, [
  contract.contractNo,
  contract.id,
  contract.tenantName,
  contract.tenant?.realName,
  contract.tenant?.username,
  contract.tenantId,
  contractStatusLabel(contract.status),
  formatDate(contract.startDate),
  formatDate(contract.endDate),
  contract.monthlyRent,
  contract.rent
])))

onMounted(() => {
  // 房源和合同数据加载完成后再计算统计信息（computeStats 依赖这两项数据）
  Promise.all([loadHouses(), loadContracts()]).then(() => computeStats())
  // 预约订单独立加载（统计信息不依赖订单数据，两者互不阻塞）
  loadOrders()
  loadReviewRecords()
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

/**
 * 当用户在房东中心内部切换标签时，同步 query.tab，确保顶部导航栏高亮保持一致。
 */
watch(
  activeTab,
  async (tab) => {
    await syncTabToRouteQuery(tab)
  }
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

/** 加载房东收到的评价记录（评价管理） */
async function loadReviewRecords() {
  reviewsLoading.value = true
  try {
    const res = await getLandlordReviewRecords({ page: 1, size: 50 })
    reviewRecords.value = Array.isArray(res) ? res : (res?.records || [])
  } catch (e) { /* ignore */ }
  finally { reviewsLoading.value = false }
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

/**
 * 根据房源当前状态判断是否应展示“上架”按钮。
 * 仅对 OFFLINE 状态提供上架入口，避免对审核中/已拒绝房源误操作。
 */
function canPutHouseOnline(status) {
  return status === 'OFFLINE'
}

/**
 * 根据房源当前状态判断是否应展示“下架”按钮。
 * ONLINE/APPROVED 视作在架状态，允许房东主动下架。
 */
function canPutHouseOffline(status) {
  return status === 'ONLINE' || status === 'APPROVED'
}

/** 房东将自己房源上架，成功后刷新“我的房源”列表 */
async function putHouseOnline(id) {
  try {
    await putMyHouseOnline(id)
    ElMessage.success('房源已上架')
    loadHouses()
  } catch (e) {
    ElMessage.error(e.message || '上架失败')
  }
}

/** 房东将自己房源下架，成功后刷新“我的房源”列表 */
async function putHouseOffline(id) {
  try {
    await putMyHouseOffline(id)
    ElMessage.success('房源已下架')
    loadHouses()
  } catch (e) {
    ElMessage.error(e.message || '下架失败')
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

/** 支付状态枚举转中文（房东端用于查看租客支付进度） */
function paymentStatusLabel(status) {
  const map = { UNPAID: '未支付', PAID: '已支付', REFUNDED: '已退款' }
  return map[status] || status || '-'
}

/** 支付状态对应 Tag 类型 */
function paymentStatusType(status) {
  const map = { UNPAID: 'warning', PAID: 'success', REFUNDED: 'info' }
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

/**
 * 获取预约订单中的房源标题（房东端）
 * 为兼容历史接口与嵌套对象结构，按 house.title -> houseTitle -> houseId 的顺序回退。
 */
function getOrderHouseTitleWithFallback(order) {
  return order?.house?.title || order?.houseTitle || (order?.houseId ? `房源#${order.houseId}` : '-')
}
</script>

<style scoped>
.landlord-center-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
  --house-table-cols: minmax(0, 1fr) minmax(120px, 160px) minmax(220px, 320px); /* info | status | actions */
  --orders-table-cols: 2fr 1.1fr 1.4fr 1fr 1fr 2.2fr; /* title | tenant | appointment | orderStatus | paymentStatus | actions */
  --contracts-table-cols: 1.6fr 1.1fr 1.5fr 1fr 1fr 1.6fr; /* number | tenant | lease | rent | status | actions */
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
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}

.table-toolbar {
  margin-bottom: 16px;
}

.search-input {
  width: 320px;
  max-width: 100%;
}

/* 我的房源表格容器：使用三列网格布局（房源信息 / 上下架状态 / 操作） */
.house-table {
  border: 1px solid #edf0f5;
  border-radius: 12px;
  overflow: hidden;
}

/* 我的房源表头与每一行复用同一套列宽，确保“状态”与“操作”垂直对齐 */
.house-table-head,
.house-table-row {
  display: grid;
  grid-template-columns: var(--house-table-cols);
  gap: 12px;
  align-items: center;
}

/* 表头样式：提供“房源信息/上下架状态/操作”导航提示 */
.house-table-head {
  background: #f8f9fc;
  color: #6b7280;
  font-size: 12px;
  font-weight: 700;
  padding: 12px 14px;
  border-bottom: 1px solid #edf0f5;
}

/* 每行数据样式 */
.house-table-row {
  padding: 14px;
  border-bottom: 1px solid #f1f3f8;
  background: #fff;
  transition: box-shadow 0.3s ease;
}

.house-table-row:last-child {
  border-bottom: none;
}

.house-table-row:hover {
  box-shadow: inset 0 0 0 1px rgba(64, 158, 255, 0.15);
}

/* 第一列：房源信息块（图 + 文） */
.house-info-col {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
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

/* 第二列：状态列居中显示，与右侧按钮列保持平行视觉关系 */
.house-status-col,
.status-col-head {
  text-align: center;
}

/* 第三列表头居中，配合按钮区域对齐 */
.action-col-head {
  text-align: center;
}

.house-item-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
}

/* 中屏适配：缩小“状态/操作”列宽，避免平板尺寸出现拥挤 */
@media (max-width: 1280px) {
  .landlord-center-page {
    --house-table-cols: minmax(0, 1fr) minmax(116px, 144px) minmax(210px, 280px);
  }
}

.table-card {
  border: 1px solid #edf0f5;
  border-radius: 12px;
  overflow: hidden;
}

.orders-table .table-head,
.orders-table .table-row {
  display: grid;
  grid-template-columns: var(--orders-table-cols);
  gap: 12px;
  align-items: center;
}

.contracts-table .table-head,
.contracts-table .table-row {
  display: grid;
  grid-template-columns: var(--contracts-table-cols);
  gap: 12px;
  align-items: center;
}

.table-head {
  background: #f8f9fc;
  color: #6b7280;
  font-size: 12px;
  font-weight: 700;
  padding: 12px 14px;
  border-bottom: 1px solid #edf0f5;
}

.table-row {
  padding: 12px 14px;
  border-bottom: 1px solid #f1f3f8;
  font-size: 13px;
  color: #4b5563;
}

.table-row:last-child {
  border-bottom: none;
}

.title-cell {
  color: #1f2937;
  font-weight: 600;
}

.row-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
}

/* 预约管理操作区按钮统一最小宽度，避免各行按钮长短不一导致视觉不齐 */
.orders-table .row-actions :deep(.el-button) {
  min-width: 88px;
  margin-left: 0;
}

/* “操作”列标题保持居中，与按钮区域对齐 */
.action-head {
  text-align: center;
}

.order-actions {
  display: flex;
  gap: 8px;
}

.orders-table :deep(.el-tag),
.contracts-table :deep(.el-tag) {
  width: fit-content;
}

@media (max-width: 1100px) {
  .house-table-row,
  .orders-table .table-head,
  .orders-table .table-row,
  .contracts-table .table-head,
  .contracts-table .table-row {
    grid-template-columns: 1fr;
    gap: 6px;
  }

  .table-head {
    display: none;
  }

  .house-table-head {
    display: none;
  }

  .table-row {
    background: #fff;
    margin-bottom: 8px;
    border: 1px solid #edf0f5;
    border-radius: 10px;
  }

  .house-table {
    border: none;
    border-radius: 0;
  }

  .house-table-row {
    margin-bottom: 8px;
    border: 1px solid #edf0f5;
    border-radius: 10px;
    padding: 12px;
  }

  .house-info-col {
    flex-direction: column;
    align-items: flex-start;
  }

  .house-status-col {
    text-align: left;
  }

  .house-item-actions {
    justify-content: flex-start;
    width: 100%;
  }
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 20px;
}

.review-list {
  display: grid;
  gap: 12px;
}

.review-item {
  border: 1px solid #edf0f5;
  border-radius: 10px;
  background: #fff;
  padding: 12px 14px;
}

.review-item-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.review-house-title {
  color: #1f2937;
  font-weight: 600;
}

.review-item-meta {
  margin-top: 8px;
  color: #8a94a6;
  font-size: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.review-item-time {
  margin-top: 8px;
  color: #8a94a6;
  font-size: 12px;
  text-align: left;
}

.review-item-content {
  margin-top: 8px;
  color: #374151;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
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
