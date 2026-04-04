<template>
  <div class="admin-house-detail-page">
    <NavBar />
    <div class="page-content" v-loading="loading">
      <div class="page-inner" v-if="house">
        <div class="page-header">
          <el-button text @click="$router.back()">
            <el-icon><ArrowLeft /></el-icon> 返回
          </el-button>
          <h2 class="page-title">房源管理详情</h2>
          <el-tag :type="houseStatusTagType(house.status)" size="large">{{ houseStatusLabel(house.status) }}</el-tag>
        </div>

        <div class="content-grid">
          <el-card class="info-card">
            <template #header>房源信息</template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="房源ID">{{ house.id }}</el-descriptions-item>
              <el-descriptions-item label="标题">{{ house.title || '-' }}</el-descriptions-item>
              <el-descriptions-item label="省市区">
                {{ house.province || '-' }} {{ house.city || '-' }} {{ house.district || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="详细地址">{{ house.address || '-' }}</el-descriptions-item>
              <el-descriptions-item label="租金(元/月)">{{ house.price ?? '-' }}</el-descriptions-item>
              <el-descriptions-item label="面积(㎡)">{{ house.area ?? '-' }}</el-descriptions-item>
              <el-descriptions-item label="户型">
                {{ house.rooms ?? 0 }}室{{ house.halls ?? 0 }}厅{{ house.bathrooms ?? 0 }}卫
              </el-descriptions-item>
              <el-descriptions-item label="楼层">
                {{ house.floor ?? '-' }}/{{ house.totalFloor ?? '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ formatDateTime(house.createTime) }}</el-descriptions-item>
              <el-descriptions-item label="更新时间">{{ formatDateTime(house.updateTime) }}</el-descriptions-item>
              <el-descriptions-item label="描述" :span="2">{{ house.description || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <el-card class="timeline-card">
            <template #header>管理员操作日志时间线</template>
            <el-timeline v-if="operationLogs.length">
              <el-timeline-item
                v-for="item in operationLogs"
                :key="item.id"
                :timestamp="formatDateTime(item.createTime)"
                :type="operationActionTagType(item.action)"
              >
                <div class="timeline-content">
                  <div class="timeline-title">管理员 {{ item.operatorName || '未知' }} 执行了“{{ item.action }}”</div>
                  <div class="timeline-remark">{{ item.remark || '无备注' }}</div>
                </div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无操作日志" />
          </el-card>
        </div>
      </div>
    </div>
    <Footer />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import NavBar from '../components/NavBar.vue'
import Footer from '../components/Footer.vue'
import { getHouseManagementDetail, getHouseOperationLogsByAdmin } from '../api/admin.js'

const route = useRoute()
const houseId = route.params.id

const loading = ref(false)
const house = ref(null)
const operationLogs = ref([])

onMounted(() => {
  loadDetail()
})

/** 加载管理员房源详情与操作日志时间线 */
async function loadDetail() {
  loading.value = true
  try {
    const [houseRes, logsRes] = await Promise.all([
      getHouseManagementDetail(houseId),
      getHouseOperationLogsByAdmin(houseId)
    ])
    house.value = houseRes || null
    operationLogs.value = Array.isArray(logsRes) ? logsRes : []
  } catch (e) {
    ElMessage.error(e.message || '加载房源详情失败')
  } finally {
    loading.value = false
  }
}

/** 房源状态中文映射 */
function houseStatusLabel(status) {
  const map = { ONLINE: '已上架', OFFLINE: '已下架', REJECTED: '已拒绝' }
  return map[status] || status
}

/** 房源状态标签类型 */
function houseStatusTagType(status) {
  const map = { ONLINE: 'success', OFFLINE: 'info', REJECTED: 'danger' }
  return map[status] || 'info'
}

/** 操作动作到时间线颜色映射，便于后续扩展更多动作类型 */
function operationActionTagType(action) {
  const map = { 上架: 'success', 下架: 'warning' }
  return map[action] || 'info'
}

/** 日期时间格式化 */
function formatDateTime(value) {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return String(value)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}
</script>

<style scoped>
.admin-house-detail-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.page-content {
  flex: 1;
  padding: 24px 16px;
  background: #f6f8fb;
}
.page-inner {
  max-width: 1200px;
  margin: 0 auto;
}
.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.page-title {
  margin: 0;
  flex: 1;
}
.content-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 16px;
}
.timeline-title {
  font-weight: 600;
}
.timeline-remark {
  margin-top: 4px;
  color: #666;
  font-size: 13px;
}
@media (max-width: 1024px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
