<template>
  <div class="admin-dashboard-page">
    <NavBar />
    <div class="page-content">
      <div class="page-inner">
        <h2 class="page-title">管理后台</h2>
        <el-tabs v-model="activeTab" class="admin-tabs">
          <!-- User Management Tab -->
          <el-tab-pane label="用户管理" name="users">
            <!--
              用户管理搜索区：
              1) 左侧关键字输入：按用户名/手机号模糊过滤；
              2) 右侧状态下拉：支持“全部/正常/已封禁”状态筛选；
              3) 输入与状态可叠加，便于管理员快速定位目标用户。
            -->
            <div class="tab-toolbar">
              <div class="toolbar-row">
                <el-input
                  v-model="userSearch"
                  placeholder="搜索用户名/手机号"
                  clearable
                  style="width:280px"
                  @input="filterUsers"
                  prefix-icon="Search"
                />
                <el-select
                  v-model="userStatusFilter"
                  clearable
                  placeholder="状态筛选"
                  style="width: 160px"
                  @change="filterUsers"
                >
                  <el-option
                    v-for="item in userStatusOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </div>
            </div>
            <el-table
              :data="filteredUsers"
              v-loading="usersLoading"
              stripe
              border
              class="data-table"
            >
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="username" label="用户名" width="130" />
              <el-table-column prop="phone" label="手机号" width="140" />
              <el-table-column prop="role" label="角色" width="100">
                <template #default="{ row }">
                  <el-tag :type="roleTagType(row.role)" size="small">{{ roleLabel(row.role) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="creditScore" label="信用分" width="100" />
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.status === 'BANNED' ? 'danger' : 'success'" size="small">
                    {{ row.status === 'BANNED' ? '已封禁' : '正常' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="140" fixed="right">
                <template #default="{ row }">
                  <el-button
                    v-if="row.status !== 'BANNED'"
                    size="small"
                    type="danger"
                    @click="handleBanUser(row)"
                  >封禁</el-button>
                  <el-button
                    v-else
                    size="small"
                    type="success"
                    @click="handleUnbanUser(row)"
                  >解封</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- House Management Tab -->
          <el-tab-pane label="房源管理" name="houseMgmt">
            <div class="tab-toolbar toolbar-row">
              <el-input
                v-model="houseMgmtKeyword"
                placeholder="搜索标题/城市/地址"
                clearable
                style="width:280px"
                @keyup.enter="loadHouseManagementList"
              />
              <!--
                房源状态筛选下拉：
                1) 支持“全部/已上架/已下架/已拒绝”；
                2) 与关键字可叠加使用，便于管理员快速筛选待处理房源；
                3) clearable 清空后回到“全部”。
              -->
              <el-select
                v-model="houseMgmtStatusFilter"
                clearable
                placeholder="上下架状态"
                style="width: 180px"
                @change="loadHouseManagementList"
              >
                <el-option
                  v-for="item in houseMgmtStatusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
              <el-button type="primary" @click="loadHouseManagementList">查询</el-button>
            </div>
            <el-table :data="houseManagementList" v-loading="houseManagementLoading" stripe border class="data-table">
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="title" label="房源标题" min-width="220" />
              <el-table-column prop="city" label="城市" width="110" />
              <el-table-column prop="district" label="区域" width="130" />
              <el-table-column prop="price" label="租金(元/月)" width="130" />
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="houseStatusTagType(row.status)" size="small">{{ houseStatusLabel(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="280" fixed="right">
                <template #default="{ row }">
                  <el-button
                    v-if="row.status !== 'ONLINE'"
                    size="small"
                    type="success"
                    @click="handlePutHouseOnline(row)"
                  >上架</el-button>
                  <el-button
                    v-if="row.status === 'ONLINE'"
                    size="small"
                    type="warning"
                    @click="handlePutHouseOffline(row)"
                  >下架</el-button>
                  <el-button size="small" @click="handleViewHouseDetail(row)">查看详情</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- Order Management Tab -->
          <el-tab-pane label="订单管理" name="orders">
            <!--
              管理员订单搜索栏：
              1) 关键字与状态下拉并列展示；
              2) 查询按钮改为与“用户管理/房源管理”一致的主色按钮样式（type=primary）；
              3) 保持回车触发、清空重载等交互不变。
            -->
            <div class="tab-toolbar">
              <div class="toolbar-row">
                <el-input
                  v-model.trim="orderKeyword"
                  clearable
                  placeholder="搜索订单（订单号）"
                  style="max-width: 360px"
                  @keyup.enter="loadOrders"
                  @clear="loadOrders"
                />
                <el-select
                  v-model="orderStatusFilter"
                  clearable
                  placeholder="状态筛选"
                  style="width: 180px"
                  @change="loadOrders"
                >
                  <el-option
                    v-for="item in orderStatusOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
                <el-button type="primary" @click="loadOrders">查询</el-button>
              </div>
            </div>
            <!--
              订单管理表格布局优化：
              1) 增加 order-table 类，便于只针对订单表格调整操作区样式；
              2) 操作列取消 fixed，避免窄屏时右侧固定列遮挡前一列内容；
              3) 操作列宽度增大并支持双按钮布局（查看 + 取消）。
            -->
            <el-table :data="orders" v-loading="ordersLoading" stripe border class="data-table order-table">
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="orderNo" label="订单编号" min-width="160" />
              <el-table-column label="房源" min-width="180">
                <template #default="{ row }">{{ row.house?.title || '-' }}</template>
              </el-table-column>
              <el-table-column label="租客" width="120">
                <template #default="{ row }">{{ row.tenant?.username || row.tenantId || '-' }}</template>
              </el-table-column>
              <el-table-column label="房东" width="120">
                <template #default="{ row }">{{ row.landlord?.username || row.landlordId || '-' }}</template>
              </el-table-column>
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="orderStatusTagType(row.status)" size="small">{{ orderStatusLabel(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="创建时间" min-width="170">
                <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="200">
                <template #default="{ row }">
                  <div class="table-action-group">
                    <el-button size="small" @click="handleViewOrderDetail(row)">查看</el-button>
                    <el-button
                      size="small"
                      type="danger"
                      plain
                      :disabled="row.status === 'CANCELLED'"
                      @click="handleCancelOrderByAdmin(row)"
                    >取消</el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- Contract Management Tab -->
          <el-tab-pane label="合同管理" name="contracts">
            <!--
              管理员合同搜索栏：
              1) 关键字与状态下拉并列展示；
              2) 查询按钮改为与“用户管理/房源管理”一致的主色按钮样式（type=primary）；
              3) 保持回车触发、清空重载等交互不变。
            -->
            <div class="tab-toolbar">
              <div class="toolbar-row">
                <el-input
                  v-model.trim="contractKeyword"
                  clearable
                  placeholder="搜索合同（合同编号/关联订单号）"
                  style="max-width: 360px"
                  @keyup.enter="loadContracts"
                  @clear="loadContracts"
                />
                <el-select
                  v-model="contractStatusFilter"
                  clearable
                  placeholder="状态筛选"
                  style="width: 200px"
                  @change="loadContracts"
                >
                  <el-option
                    v-for="item in contractStatusOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
                <el-button type="primary" @click="loadContracts">查询</el-button>
              </div>
            </div>
            <!--
              合同管理表格：
              1) 追加 contract-table 类，仅对合同表格做样式微调，避免影响其它管理表格；
              2) 将“操作”列取消 fixed="right"，避免在窄视口下覆盖“创建时间”列内容。
            -->
            <el-table :data="contracts" v-loading="contractsLoading" stripe border class="data-table contract-table">
              <el-table-column prop="id" label="ID" width="80" />
              <!--
                合同编号列：通过 header/cell class 单独缩小字体，
                让长编号在同等宽度下展示更多字符，减少操作列被挤压概率。
              -->
              <el-table-column
                prop="contractNo"
                label="合同编号"
                min-width="150"
                header-cell-class-name="contract-compact-header"
                cell-class-name="contract-compact-cell"
              />
              <!--
                关联订单列：与合同编号列保持一致的紧凑字体策略，
                统一视觉密度并降低表格横向挤压。
              -->
              <el-table-column
                prop="orderNo"
                label="关联订单"
                min-width="140"
                header-cell-class-name="contract-compact-header"
                cell-class-name="contract-compact-cell"
              />
              <el-table-column label="房源" min-width="160">
                <template #default="{ row }">{{ row.house?.title || '-' }}</template>
              </el-table-column>
              <el-table-column label="租客" width="120">
                <template #default="{ row }">{{ row.tenant?.username || row.tenantId || '-' }}</template>
              </el-table-column>
              <el-table-column label="房东" width="120">
                <template #default="{ row }">{{ row.landlord?.username || row.landlordId || '-' }}</template>
              </el-table-column>
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="contractStatusTagType(row.status)" size="small">{{ contractStatusLabel(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <!--
                创建时间列：适当增大最小宽度并开启溢出提示，
                防止时间字符串被截断或在紧凑布局下显示不全。
              -->
              <el-table-column label="创建时间" min-width="170" show-overflow-tooltip>
                <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
              </el-table-column>
              <!--
                操作列：不再右侧固定，避免 fixed 列在窄屏时遮挡前一列（创建时间）的问题。
              -->
              <!--
                合同操作列扩宽并启用最小宽度：
                1) 给“查看/取消”留出稳定展示空间，避免在中等分辨率下被压缩遮挡；
                2) 保持非 fixed 布局，继续规避 fixed 列覆盖相邻列的问题。
              -->
              <el-table-column label="操作" min-width="220" class-name="contract-actions-column">
                <template #default="{ row }">
                  <div class="table-action-group">
                    <el-button size="small" text @click="handleViewContractDetail(row)">查看</el-button>
                    <el-button
                      size="small"
                      type="danger"
                      text
                      :disabled="row.status === 'CANCELLED' || row.status === 'FULLY_SIGNED'"
                      @click="handleCancelContractByAdmin(row)"
                    >取消</el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- Overview Tab -->
          <el-tab-pane label="数据概览" name="overview">
            <div class="stats-cards">
              <el-card class="stat-card">
                <div class="stat-icon blue"><el-icon><User /></el-icon></div>
                <div class="stat-info">
                  <div class="stat-num">{{ stats.userCount || 0 }}</div>
                  <div class="stat-label">总用户数</div>
                </div>
              </el-card>
              <el-card class="stat-card">
                <div class="stat-icon green"><el-icon><House /></el-icon></div>
                <div class="stat-info">
                  <div class="stat-num">{{ stats.houseCount || 0 }}</div>
                  <div class="stat-label">总房源数</div>
                </div>
              </el-card>
              <el-card class="stat-card">
                <div class="stat-icon orange"><el-icon><Clock /></el-icon></div>
                <div class="stat-info">
                  <div class="stat-num">{{ stats.pendingContracts || 0 }}</div>
                  <div class="stat-label">待审核合同</div>
                </div>
              </el-card>
              <el-card class="stat-card">
                <div class="stat-icon purple"><el-icon><Document /></el-icon></div>
                <div class="stat-info">
                  <div class="stat-num">{{ stats.contractCount || 0 }}</div>
                  <div class="stat-label">成交合同数</div>
                </div>
              </el-card>
            </div>

            <div class="charts-grid">
              <el-card class="chart-card">
                <template #header>各城市房源数量</template>
                <div ref="areaChartRef" class="chart-container"></div>
              </el-card>
              <el-card class="chart-card">
                <template #header>近6个月价格趋势</template>
                <div ref="priceChartRef" class="chart-container"></div>
              </el-card>
              <el-card class="chart-card full-width">
                <template #header>用户信用分布</template>
                <div ref="creditChartRef" class="chart-container"></div>
              </el-card>
            </div>
          </el-tab-pane>

        </el-tabs>
      </div>
    </div>

    <Footer />

  </div>
</template>

<script setup>
// 说明：管理后台页逻辑，仅限 ADMIN 角色访问，提供概览、用户、房源管理、订单、合同管理功能
import { ref, onMounted, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'  // ECharts 图表库（用于柱状图、折线图、饼图）
import NavBar from '../components/NavBar.vue'
import Footer from '../components/Footer.vue'
import {
  getStats,
  getUserList,
  getAreaStats,
  getPriceTrends,
  getCreditDistribution,
  getHouseManagementList,
  putHouseOnlineByAdmin,
  putHouseOfflineByAdmin,
  getOrderList,
  getContractList,
  cancelOrderByAdmin,
  cancelContractByAdmin,
  banUser,
  unbanUser
} from '../api/admin.js'

const activeTab = ref('overview')        // 当前激活 tab
const router = useRouter()               // 路由实例（用于跳转房源详情页）
const route = useRoute()                 // 当前路由对象（用于读取/同步 ?tab=xxx）
const DEFAULT_ADMIN_PAGE_SIZE = 100       // 后台管理列表默认一次拉取数量
const stats = ref({})                    // 平台概览统计数据
const users = ref([])                    // 所有用户列表（未过滤）
const filteredUsers = ref([])            // 关键词过滤后的用户列表（用于表格展示）
const usersLoading = ref(false)          // 用户列表加载状态
const userSearch = ref('')               // 用户搜索关键词
const userStatusFilter = ref('')         // 用户状态筛选（通过 clearable 清空后回到“全部”）
const userStatusOptions = [              // 用户状态下拉选项
  { label: '正常', value: 'ACTIVE' },
  { label: '已封禁', value: 'BANNED' }
]

const houseManagementList = ref([])       // 房源管理列表
const houseManagementLoading = ref(false) // 房源管理加载状态
const houseMgmtKeyword = ref('')          // 房源管理关键词
const houseMgmtStatusFilter = ref('')     // 房源管理状态筛选（ONLINE/OFFLINE/REJECTED）
const houseMgmtStatusOptions = [          // 房源状态下拉选项（与后端白名单保持一致）
  { label: '已上架', value: 'ONLINE' },
  { label: '已下架', value: 'OFFLINE' },
  { label: '已拒绝', value: 'REJECTED' }
]

const orders = ref([])                   // 管理员订单列表
const ordersLoading = ref(false)         // 订单列表加载状态
const orderKeyword = ref('')             // 管理员订单搜索关键字（订单号）
const orderStatusFilter = ref('')        // 管理员订单状态筛选
const orderStatusOptions = [             // 订单状态下拉选项（文案与后端状态枚举一一对应）
  { label: '待处理', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已拒绝', value: 'REJECTED' },
  { label: '已取消', value: 'CANCELLED' },
  { label: '已完成', value: 'COMPLETED' }
]

const contracts = ref([])                // 管理员合同列表
const contractsLoading = ref(false)      // 合同列表加载状态
const contractKeyword = ref('')          // 管理员合同搜索关键字（合同编号/关联订单号）
const contractStatusFilter = ref('')     // 管理员合同状态筛选
const contractStatusOptions = [          // 合同状态下拉选项（文案与后端状态枚举一一对应）
  { label: '草稿', value: 'DRAFT' },
  { label: '待签署', value: 'PENDING_SIGN' },
  { label: '租客已签', value: 'TENANT_SIGNED' },
  { label: '房东已签', value: 'LANDLORD_SIGNED' },
  { label: '双方已签', value: 'FULLY_SIGNED' },
  { label: '已取消', value: 'CANCELLED' }
]

// ECharts 图表 DOM 引用
const areaChartRef = ref(null)   // 城市房源数量柱状图容器
const priceChartRef = ref(null)  // 租金趋势折线图容器
const creditChartRef = ref(null) // 信用分布饼图容器
// 管理后台可切换标签白名单：用于约束 query.tab 合法值，避免异常参数污染界面状态
const allowedAdminTabs = ['overview', 'users', 'houseMgmt', 'orders', 'contracts']

/**
 * 将管理后台当前标签同步到 URL query.tab：
 * 1) 让顶部导航栏（/admin?tab=xxx）高亮与后台标签保持一致；
 * 2) 支持刷新后仍停留在当前模块。
 */
function syncAdminTabToRouteQuery(tabName) {
  const targetTab = typeof tabName === 'string' ? tabName : ''
  if (!allowedAdminTabs.includes(targetTab)) return
  if (route.query.tab === targetTab) return
  const nextQuery = { ...route.query, tab: targetTab }
  router.replace({ path: route.path, query: nextQuery })
}

onMounted(async () => {
  // 并发加载统计数据、用户列表
  loadStats()
  loadUsers()
  loadHouseManagementList()
  loadOrders()
  loadContracts()
  // 等待 DOM 渲染完成后再初始化 ECharts 图表（避免容器尺寸为 0）
  await nextTick()
  setTimeout(() => {
    initCharts()
  }, 200)  // 额外延迟确保 tab 切换后 DOM 完全渲染
})

/**
 * 监听 query.tab：
 * - 支持从顶部导航栏点击“用户管理/房源管理/订单管理/合同管理/数据概览”后自动切换对应标签；
 * - 对无效 tab 参数直接忽略，避免页面抖动或进入不存在标签。
 */
watch(
  () => route.query.tab,
  (tab) => {
    const targetTab = typeof tab === 'string' ? tab : ''
    if (!targetTab) {
      activeTab.value = 'overview'
      // 当 URL 未携带 tab 时，补齐默认 tab=overview，避免地址与当前标签不一致。
      syncAdminTabToRouteQuery('overview')
      return
    }
    if (!allowedAdminTabs.includes(targetTab)) return
    activeTab.value = targetTab
  },
  { immediate: true }
)

// 在后台内部切换标签时，同步 query.tab，确保与顶部导航双向联动。
watch(
  activeTab,
  (tab) => {
    syncAdminTabToRouteQuery(tab)
  }
)

/** 加载平台整体数据概览统计 */
async function loadStats() {
  try {
    const res = await getStats()
    stats.value = res || {}
  } catch (e) { /* ignore */ }
}

/** 加载所有用户列表（支持关键词+状态前端过滤） */
async function loadUsers() {
  usersLoading.value = true
  try {
    const res = await getUserList({ page: 1, size: DEFAULT_ADMIN_PAGE_SIZE })
    // 后端返回 PageResult 对象，其数据列表字段为 records（非 list）。
    users.value = Array.isArray(res) ? res : (res?.records || [])
    // 列表加载完成后执行一次统一过滤逻辑，保证筛选状态与展示结果一致。
    filterUsers()
  } catch (e) { /* ignore */ }
  finally { usersLoading.value = false }
}

/** 加载房源管理列表（支持关键词，覆盖上架/下架/查看详情场景） */
async function loadHouseManagementList() {
  houseManagementLoading.value = true
  try {
    const res = await getHouseManagementList({
      page: 1,
      size: DEFAULT_ADMIN_PAGE_SIZE,
      keyword: houseMgmtKeyword.value || undefined,
      status: houseMgmtStatusFilter.value || undefined
    })
    houseManagementList.value = Array.isArray(res) ? res : (res?.records || [])
  } catch (e) {
    ElMessage.error(e.message || '加载房源管理列表失败')
  } finally {
    houseManagementLoading.value = false
  }
}

/**
 * 加载管理员订单列表（支持关键词）：
 * - keyword 为空：按创建时间倒序返回订单；
 * - keyword 非空：后端按订单号与状态做筛选，便于管理员快速定位目标订单。
 */
async function loadOrders() {
  ordersLoading.value = true
  try {
    const res = await getOrderList({
      page: 1,
      size: DEFAULT_ADMIN_PAGE_SIZE,
      keyword: orderKeyword.value || undefined,
      status: orderStatusFilter.value || undefined
    })
    orders.value = Array.isArray(res) ? res : (res?.records || [])
  } catch (e) {
    ElMessage.error(e.message || '加载订单失败')
  } finally {
    ordersLoading.value = false
  }
}

/** 加载管理员合同列表（支持关键词+状态筛选） */
async function loadContracts() {
  contractsLoading.value = true
  try {
    const res = await getContractList({
      page: 1,
      size: DEFAULT_ADMIN_PAGE_SIZE,
      keyword: contractKeyword.value || undefined,
      status: contractStatusFilter.value || undefined
    })
    contracts.value = Array.isArray(res) ? res : (res?.records || [])
  } catch (e) {
    ElMessage.error(e.message || '加载合同失败')
  } finally {
    contractsLoading.value = false
  }
}

/**
 * 前端实时过滤用户列表（关键字 + 状态组合筛选）
 * 触发时机：
 * 1) 搜索框输入变化；
 * 2) 状态下拉变化；
 * 3) 用户列表初次加载完成。
 */
function filterUsers() {
  const keyword = userSearch.value.trim().toLowerCase()
  const status = userStatusFilter.value || ''
  filteredUsers.value = users.value.filter((u) => {
    const hitKeyword = keyword
      ? (u.username?.toLowerCase().includes(keyword) || u.phone?.includes(keyword))
      : true
    const hitStatus = status ? u.status === status : true
    return hitKeyword && hitStatus
  })
}

/**
 * 初始化 ECharts 图表：城市房源柱状图、租金趋势折线图、信用分布饼图
 * 优先使用接口数据，接口失败时回退到硬编码的示例数据
 */
async function initCharts() {
  try {
    // 并发请求三个图表数据，任一失败不阻塞其他
    const [areaRes, priceRes, creditRes] = await Promise.allSettled([
      getAreaStats(),
      getPriceTrends(),
      getCreditDistribution()
    ])

    // 初始化城市房源数量柱状图
    if (areaChartRef.value) {
      const areaChart = echarts.init(areaChartRef.value)
      const areaData = areaRes.status === 'fulfilled' ? (areaRes.value || []) : []
      areaChart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: {
          type: 'category',
          data: areaData.map(d => d.city || d.name || '未知'),
          axisLabel: { rotate: 30 }  // 城市名称倾斜显示，避免重叠
        },
        yAxis: { type: 'value', name: '房源数' },
        series: [{
          name: '房源数量',
          type: 'bar',
          data: areaData.map(d => d.count || d.value || 0),
          itemStyle: { color: '#409eff' }
        }],
        grid: { bottom: 60 }
      })
    }

    // 初始化近6个月租金均价折线图
    if (priceChartRef.value) {
      const priceChart = echarts.init(priceChartRef.value)
      const priceData = priceRes.status === 'fulfilled' ? (priceRes.value || []) : []
      priceChart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: {
          type: 'category',
          data: priceData.map(d => d.month || d.date || ''),
        },
        yAxis: { type: 'value', name: '均价(元/月)' },
        series: [{
          name: '平均租金',
          type: 'line',
          smooth: true,   // 平滑曲线
          data: priceData.map(d => d.avgPrice || d.price || 0),
          itemStyle: { color: '#67c23a' },
          areaStyle: { opacity: 0.15 }  // 面积填充
        }]
      })
    }

    // 初始化用户信用分分布饼图（环形图）
    if (creditChartRef.value) {
      const creditChart = echarts.init(creditChartRef.value)
      const creditData = creditRes.status === 'fulfilled' ? (creditRes.value || []) : []
      creditChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { orient: 'vertical', left: 'left' },
        series: [{
          name: '信用分布',
          type: 'pie',
          radius: ['40%', '70%'],  // 环形图：内径40%，外径70%
          // 优先使用接口数据，否则使用默认示例数据
          data: creditData.length > 0 ? creditData.map(d => ({
            name: d.range || d.name || '未知',
            value: d.count || d.value || 0
          })) : [
            { name: '90-100(优秀)', value: 45 },
            { name: '70-89(良好)', value: 30 },
            { name: '60-69(一般)', value: 15 },
            { name: '60以下(较低)', value: 10 }
          ]
        }]
      })
    }
  } catch (e) {
    // 图表初始化失败时使用兜底示例数据渲染城市柱状图
    if (areaChartRef.value) {
      const areaChart = echarts.init(areaChartRef.value)
      areaChart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: ['北京', '上海', '广州', '深圳', '杭州', '成都'] },
        yAxis: { type: 'value' },
        series: [{ type: 'bar', data: [120, 200, 150, 80, 70, 110], itemStyle: { color: '#409eff' } }]
      })
    }
  }
}

/**
 * 封禁指定用户账号
 * @param {Object} user - 被封禁的用户对象
 */
async function handleBanUser(user) {
  try {
    await banUser(user.id)
    user.status = 'BANNED'  // 本地更新状态，无需重新请求列表
    ElMessage.success(`已封禁用户 ${user.username}`)
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

/**
 * 解封指定用户账号
 * @param {Object} user - 被解封的用户对象
 */
async function handleUnbanUser(user) {
  try {
    await unbanUser(user.id)
    user.status = 'ACTIVE'
    ElMessage.success(`已解封用户 ${user.username}`)
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

/** 管理员上架房源 */
async function handlePutHouseOnline(house) {
  try {
    await putHouseOnlineByAdmin(house.id)
    ElMessage.success('房源已上架，系统将通知相关用户')
    loadHouseManagementList()
  } catch (e) {
    ElMessage.error(e.message || '上架失败')
  }
}

/** 管理员下架房源 */
async function handlePutHouseOffline(house) {
  try {
    await putHouseOfflineByAdmin(house.id)
    ElMessage.success('房源已下架，系统将通知相关用户')
    loadHouseManagementList()
  } catch (e) {
    ElMessage.error(e.message || '下架失败')
  }
}

/** 查看管理员房源管理详情（跳转到独立详情页） */
function handleViewHouseDetail(house) {
  router.push(`/admin/houses/${house.id}`)
}

/**
 * 查看订单详情：
 * - 管理员复用系统统一订单详情页，保持与租客/房东一致的信息结构；
 * - 仅负责跳转，不在列表页额外请求详情数据，避免重复网络开销。
 */
function handleViewOrderDetail(orderRow) {
  router.push(`/orders/${orderRow.id}`)
}

/**
 * 查看合同详情：
 * - 管理员复用系统统一合同详情页，直接查看对应合同完整条款与签署状态；
 * - 列表页点击后路由跳转，由详情页自行拉取完整合同数据。
 */
function handleViewContractDetail(contractRow) {
  router.push(`/contracts/${contractRow.id}`)
}

/**
 * 格式化日期时间，解决后端 ISO 字符串中 “T” 直出问题：
 * 例如 2026-04-06T18:30:00 -> 2026-04-06 18:30:00
 */
function formatDateTime(dateTime) {
  if (!dateTime) return '-'
  // 为保持模板语义清晰，这里保留 formatDateTime 命名：
  // 对 ISO 字符串做标准化处理，输出更符合中文后台展示习惯的日期时间文本。
  const raw = String(dateTime).trim()
  // 仅在标准 ISO 时间字符串场景下执行“T”替换与毫秒裁剪，避免对非时间文本误处理。
  if (/^\d{4}-\d{2}-\d{2}T/.test(raw)) {
    return raw
      .replace('T', ' ')
      .replace(/\.\d+Z?$/, '')
      .replace(/Z$/, '')
  }
  return raw
}

/** 管理员取消订单（取消后刷新列表，保证状态即时可见） */
async function handleCancelOrderByAdmin(order) {
  try {
    await cancelOrderByAdmin(order.id)
    ElMessage.success('订单已取消')
    loadOrders()
  } catch (e) {
    ElMessage.error(e.message || '取消订单失败')
  }
}

/** 管理员取消合同（取消后刷新列表，保持合同与房源状态展示一致） */
async function handleCancelContractByAdmin(contract) {
  try {
    await cancelContractByAdmin(contract.id)
    ElMessage.success('合同已取消')
    loadContracts()
  } catch (e) {
    ElMessage.error(e.message || '取消合同失败')
  }
}

/** 用户角色枚举转中文标签 */
function roleLabel(role) {
  const map = { TENANT: '租客', LANDLORD: '房东', ADMIN: '管理员' }
  return map[role] || role
}

/** 用户角色对应的 Tag 类型 */
function roleTagType(role) {
  const map = { TENANT: '', LANDLORD: 'success', ADMIN: 'danger' }
  return map[role] || 'info'
}

/** 房源状态枚举转中文 */
function houseStatusLabel(status) {
  const map = { ONLINE: '已上架', OFFLINE: '已下架', REJECTED: '已拒绝' }
  return map[status] || status
}

/** 房源状态对应的 Tag 类型 */
function houseStatusTagType(status) {
  const map = { ONLINE: 'success', OFFLINE: 'info', REJECTED: 'danger' }
  return map[status] || 'info'
}

/** 订单状态枚举转中文 */
function orderStatusLabel(status) {
  const map = { PENDING: '待处理', APPROVED: '已通过', SIGNED: '已签约', REJECTED: '已拒绝', CANCELLED: '已取消', COMPLETED: '已完成' }
  return map[status] || status
}

/** 订单状态对应 Tag 类型 */
function orderStatusTagType(status) {
  const map = { PENDING: 'warning', APPROVED: 'success', SIGNED: 'success', REJECTED: 'danger', CANCELLED: 'info', COMPLETED: 'primary' }
  return map[status] || 'info'
}

/** 合同状态枚举转中文 */
function contractStatusLabel(status) {
  const map = { DRAFT: '草稿', PENDING_SIGN: '待签署', TENANT_SIGNED: '租客已签', LANDLORD_SIGNED: '房东已签', FULLY_SIGNED: '双方已签', CANCELLED: '已取消' }
  return map[status] || status
}

/** 合同状态对应 Tag 类型 */
function contractStatusTagType(status) {
  const map = { DRAFT: 'info', PENDING_SIGN: 'warning', TENANT_SIGNED: 'warning', LANDLORD_SIGNED: 'warning', FULLY_SIGNED: 'success', CANCELLED: 'danger' }
  return map[status] || 'info'
}
</script>

<style scoped>
.admin-dashboard-page {
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
  max-width: 1200px;
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

.admin-tabs {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  flex-shrink: 0;
}

.stat-icon.blue { background: #ecf5ff; color: #409eff; }
.stat-icon.green { background: #f0f9eb; color: #67c23a; }
.stat-icon.orange { background: #fdf6ec; color: #e6a23c; }
.stat-icon.purple { background: #f5f0ff; color: #9c59d1; }

.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.charts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.chart-card {
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}

.chart-card.full-width {
  grid-column: 1 / -1;
}

.chart-container {
  height: 300px;
  width: 100%;
}

.tab-toolbar {
  margin-bottom: 16px;
}

.toolbar-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.data-table {
  width: 100%;
}

/*
  管理表格操作按钮组：
  - 通过弹性布局让“查看/取消”在狭窄列宽下自动换行，避免按钮互相遮挡；
  - 统一订单管理与合同管理操作列的按钮间距和可读性。
*/
:deep(.table-action-group) {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

/*
  合同表格紧凑字体样式（仅作用于合同管理 tab）：
  - 目的：缩小“合同编号/关联订单”字号，减少横向空间占用；
  - 范围：通过 .contract-table 前缀限定，避免影响用户、订单等其它表格。
*/
:deep(.contract-table .contract-compact-header .cell) {
  font-size: 12px;
}

:deep(.contract-table .contract-compact-cell .cell) {
  font-size: 12px;
}

/*
  订单管理操作列优化：
  - 操作区按钮文本不换行，避免“查看/取消”被压缩到不可读；
  - 仅作用在订单管理表格，防止影响其它页面的按钮行为。
*/
:deep(.order-table .table-action-group .el-button) {
  white-space: nowrap;
}

/*
  合同管理操作列优化：
  - 与订单管理保持一致的按钮布局策略，解决右侧按钮被遮挡问题；
  - 限定在合同表格作用域内，确保样式变更可控。
*/
:deep(.contract-table .table-action-group .el-button) {
  white-space: nowrap;
  margin-left: 0;
}

/*
  合同管理操作列增强：
  - 显式保证操作列单元格内容可见，不因默认溢出策略导致按钮被裁切；
  - 在窄宽度下允许按钮组换行，优先保证“取消”按钮可点击。
*/
:deep(.contract-table .contract-actions-column) {
  overflow: visible;
}

:deep(.contract-table .table-action-group) {
  min-width: 170px;
}

.empty-audit {
  padding: 40px 0;
}

@media (max-width: 768px) {
  .charts-grid {
    grid-template-columns: 1fr;
  }

  .chart-card.full-width {
    grid-column: auto;
  }
}
</style>
