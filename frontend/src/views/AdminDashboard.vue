<template>
  <div class="admin-dashboard-page">
    <NavBar />
    <div class="page-content">
      <div class="page-inner">
        <h2 class="page-title">管理后台</h2>
        <el-tabs v-model="activeTab" class="admin-tabs">
          <!-- Overview Tab -->
          <el-tab-pane label="数据概览" name="overview">
            <div class="stats-cards">
              <el-card class="stat-card">
                <div class="stat-icon blue"><el-icon><User /></el-icon></div>
                <div class="stat-info">
                  <div class="stat-num">{{ stats.totalUsers || 0 }}</div>
                  <div class="stat-label">总用户数</div>
                </div>
              </el-card>
              <el-card class="stat-card">
                <div class="stat-icon green"><el-icon><House /></el-icon></div>
                <div class="stat-info">
                  <div class="stat-num">{{ stats.totalHouses || 0 }}</div>
                  <div class="stat-label">总房源数</div>
                </div>
              </el-card>
              <el-card class="stat-card">
                <div class="stat-icon orange"><el-icon><Clock /></el-icon></div>
                <div class="stat-info">
                  <div class="stat-num">{{ stats.pendingAudit || 0 }}</div>
                  <div class="stat-label">待审核</div>
                </div>
              </el-card>
              <el-card class="stat-card">
                <div class="stat-icon purple"><el-icon><Document /></el-icon></div>
                <div class="stat-info">
                  <div class="stat-num">{{ stats.totalContracts || 0 }}</div>
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

          <!-- User Management Tab -->
          <el-tab-pane label="用户管理" name="users">
            <div class="tab-toolbar">
              <el-input
                v-model="userSearch"
                placeholder="搜索用户名/手机号"
                clearable
                style="width:280px"
                @input="filterUsers"
                prefix-icon="Search"
              />
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
                  <el-tag :type="row.banned ? 'danger' : 'success'" size="small">
                    {{ row.banned ? '已封禁' : '正常' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="140" fixed="right">
                <template #default="{ row }">
                  <el-button
                    v-if="!row.banned"
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

          <!-- House Audit Tab -->
          <el-tab-pane label="房源审核" name="audit">
            <el-table
              :data="pendingHouses"
              v-loading="housesLoading"
              stripe
              border
              class="data-table"
            >
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
              <el-table-column prop="landlordName" label="房东" width="120" />
              <el-table-column prop="city" label="城市" width="100" />
              <el-table-column prop="price" label="价格(元/月)" width="120" />
              <el-table-column prop="ownerType" label="房东类型" width="120">
                <template #default="{ row }">
                  <el-tag size="small">{{ ownerTypeLabel(row.ownerType) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="180" fixed="right">
                <template #default="{ row }">
                  <el-button size="small" type="success" @click="approveHouse(row)">通过</el-button>
                  <el-button size="small" type="danger" @click="openAuditReject(row)">拒绝</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <!-- Audit Reject Dialog -->
    <el-dialog v-model="auditDialogVisible" title="拒绝房源" width="400px">
      <el-form>
        <el-form-item label="拒绝原因">
          <el-input
            v-model="auditReason"
            type="textarea"
            :rows="3"
            placeholder="请输入拒绝原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="auditing" @click="submitAuditReject">确认拒绝</el-button>
      </template>
    </el-dialog>

    <Footer />
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import NavBar from '../components/NavBar.vue'
import Footer from '../components/Footer.vue'
import { getStats, getUserList, getPendingHouses, auditHouseAdmin, getAreaStats, getPriceTrends, getCreditDistribution, banUser, unbanUser } from '../api/admin.js'

const activeTab = ref('overview')
const stats = ref({})
const users = ref([])
const filteredUsers = ref([])
const pendingHouses = ref([])
const usersLoading = ref(false)
const housesLoading = ref(false)
const userSearch = ref('')
const auditDialogVisible = ref(false)
const auditReason = ref('')
const auditing = ref(false)
const currentAuditHouse = ref(null)

const areaChartRef = ref(null)
const priceChartRef = ref(null)
const creditChartRef = ref(null)

onMounted(async () => {
  loadStats()
  loadUsers()
  loadPendingHouses()
  await nextTick()
  setTimeout(() => {
    initCharts()
  }, 200)
})

async function loadStats() {
  try {
    const res = await getStats()
    stats.value = res || {}
  } catch (e) { /* ignore */ }
}

async function loadUsers() {
  usersLoading.value = true
  try {
    const res = await getUserList({ page: 1, pageSize: 100 })
    users.value = Array.isArray(res) ? res : (res?.list || [])
    filteredUsers.value = [...users.value]
  } catch (e) { /* ignore */ }
  finally { usersLoading.value = false }
}

async function loadPendingHouses() {
  housesLoading.value = true
  try {
    const res = await getPendingHouses({ page: 1, pageSize: 100 })
    pendingHouses.value = Array.isArray(res) ? res : (res?.list || [])
  } catch (e) { /* ignore */ }
  finally { housesLoading.value = false }
}

function filterUsers() {
  const keyword = userSearch.value.toLowerCase()
  filteredUsers.value = users.value.filter(u =>
    u.username?.toLowerCase().includes(keyword) || u.phone?.includes(keyword)
  )
}

async function initCharts() {
  try {
    const [areaRes, priceRes, creditRes] = await Promise.allSettled([
      getAreaStats(),
      getPriceTrends(),
      getCreditDistribution()
    ])

    if (areaChartRef.value) {
      const areaChart = echarts.init(areaChartRef.value)
      const areaData = areaRes.status === 'fulfilled' ? (areaRes.value || []) : []
      areaChart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: {
          type: 'category',
          data: areaData.map(d => d.city || d.name || '未知'),
          axisLabel: { rotate: 30 }
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
          smooth: true,
          data: priceData.map(d => d.avgPrice || d.price || 0),
          itemStyle: { color: '#67c23a' },
          areaStyle: { opacity: 0.15 }
        }]
      })
    }

    if (creditChartRef.value) {
      const creditChart = echarts.init(creditChartRef.value)
      const creditData = creditRes.status === 'fulfilled' ? (creditRes.value || []) : []
      creditChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { orient: 'vertical', left: 'left' },
        series: [{
          name: '信用分布',
          type: 'pie',
          radius: ['40%', '70%'],
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
    // Charts init failed, use fallback data
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

async function handleBanUser(user) {
  try {
    await banUser(user.id)
    user.banned = true
    ElMessage.success(`已封禁用户 ${user.username}`)
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleUnbanUser(user) {
  try {
    await unbanUser(user.id)
    user.banned = false
    ElMessage.success(`已解封用户 ${user.username}`)
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function approveHouse(house) {
  try {
    await auditHouseAdmin(house.id, { status: 'APPROVED' })
    ElMessage.success('已通过审核')
    pendingHouses.value = pendingHouses.value.filter(h => h.id !== house.id)
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

function openAuditReject(house) {
  currentAuditHouse.value = house
  auditReason.value = ''
  auditDialogVisible.value = true
}

async function submitAuditReject() {
  auditing.value = true
  try {
    await auditHouseAdmin(currentAuditHouse.value.id, { status: 'REJECTED', reason: auditReason.value })
    ElMessage.success('已拒绝该房源')
    pendingHouses.value = pendingHouses.value.filter(h => h.id !== currentAuditHouse.value.id)
    auditDialogVisible.value = false
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    auditing.value = false
  }
}

function roleLabel(role) {
  const map = { TENANT: '租客', LANDLORD: '房东', ADMIN: '管理员' }
  return map[role] || role
}

function roleTagType(role) {
  const map = { TENANT: '', LANDLORD: 'success', ADMIN: 'danger' }
  return map[role] || 'info'
}

function ownerTypeLabel(type) {
  const map = { OWNER: '一手房东', SUBLEASE: '二手房东', AGENT: '持牌中介' }
  return map[type] || type
}
</script>

<style scoped>
.admin-dashboard-page {
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
  max-width: 1200px;
  margin: 0 auto;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 20px;
}

.admin-tabs {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
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
  border-radius: 12px;
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
  border-radius: 12px;
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

.data-table {
  width: 100%;
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
