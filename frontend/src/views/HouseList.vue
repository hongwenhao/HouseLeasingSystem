<template>
  <!-- 组件说明：房源列表页，左侧为搜索筛选栏，右侧为房源卡片网格。
       支持多条件筛选（城市/区域/价格区间/户型/房东类型/装修）、
       排序（默认/价格升降序/最新）以及分页浏览。
       搜索条件同步到 URL query 参数，支持刷新后保留筛选状态。 -->
  <div class="house-list-page">
    <NavBar />
    <div class="page-content">
      <!-- 左侧搜索筛选侧边栏（小屏下展开为顶部条） -->
      <div class="sidebar">
        <SearchBar v-model="filters" @search="handleSearch" />
      </div>
      <!-- 右侧主内容区 -->
      <div class="main-content">
        <!-- 工具栏：结果数量 + 排序方式 -->
        <div class="toolbar">
          <div class="count-info">
            共找到 <strong>{{ total }}</strong> 套房源
          </div>
          <!-- 排序按钮组 -->
          <div class="sort-btns">
            <el-radio-group v-model="sortBy" size="small" @change="fetchData">
              <el-radio-button value="">默认</el-radio-button>
              <el-radio-button value="price_asc">价格升序</el-radio-button>
              <el-radio-button value="price_desc">价格降序</el-radio-button>
              <el-radio-button value="newest">最新</el-radio-button>
            </el-radio-group>
          </div>
        </div>

        <!-- 加载状态：骨架屏 -->
        <div v-if="loading" class="loading-wrap">
          <el-skeleton :rows="6" animated />
        </div>
        <!-- 房源网格列表 -->
        <div v-else-if="houses.length > 0" class="house-grid">
          <HouseCard
            v-for="house in houses"
            :key="house.id"
            :house="house"
          />
        </div>
        <!-- 暂无结果 -->
        <el-empty v-else description="未找到符合条件的房源" class="empty-state" />

        <!-- 分页控件（仅在总数超过每页数量时显示） -->
        <div class="pagination-wrap" v-if="total > pageSize">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[12, 24, 36]"
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="fetchData"
            @size-change="fetchData"
          />
        </div>
      </div>
    </div>
    <Footer />
  </div>
</template>

<script setup>
// 说明：房源列表页逻辑，管理筛选条件、分页和排序状态，并驱动列表数据加载
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import NavBar from '../components/NavBar.vue'
import Footer from '../components/Footer.vue'
import SearchBar from '../components/SearchBar.vue'
import HouseCard from '../components/HouseCard.vue'
import { getHouses } from '../api/house.js'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const houses = ref([])   // 当前页房源列表
const total = ref(0)     // 总条数（用于分页组件）
const currentPage = ref(1)
const pageSize = ref(12)
const sortBy = ref('')   // 排序方式：'' / price_asc / price_desc / newest

// 从 URL query 参数初始化筛选条件（支持刷新/分享链接保留状态）
const filters = reactive({
  keyword: route.query.keyword || '',
  province: route.query.province || '',
  city: route.query.city || '',
  district: route.query.district || '',
  minPrice: route.query.minPrice ? Number(route.query.minPrice) : null,
  maxPrice: route.query.maxPrice ? Number(route.query.maxPrice) : null,
  rooms: route.query.rooms || '',
  ownerType: route.query.ownerType || '',
  decoration: route.query.decoration || ''
})

/**
 * 获取房源列表数据
 * 将当前分页、排序和筛选条件合并为请求参数，并同步更新 URL query
 */
async function fetchData() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      sortBy: sortBy.value
    }
    // 将非空筛选条件合并到请求参数（province 为前端导航辅助字段，不发送给后端）
    Object.entries(filters).forEach(([k, v]) => {
      if (k !== 'province' && v !== '' && v !== null && v !== undefined) params[k] = v
    })
    updateQuery(params)  // 同步 URL
    const res = await getHouses(params)
    if (res && res.records !== undefined) {
      houses.value = res.records
      total.value = res.total ?? res.records.length
    } else if (res && res.list !== undefined) {
      houses.value = res.list
      total.value = res.total
    } else if (Array.isArray(res)) {
      houses.value = res
      total.value = res.length
    } else {
      houses.value = []
      total.value = 0
    }
  } catch (e) {
    ElMessage.error('加载房源列表失败')
  } finally {
    loading.value = false
  }
}

/**
 * 将非分页类参数同步到 URL query，方便用户分享和刷新保留状态
 * @param {Object} params - 请求参数对象
 */
function updateQuery(params) {
  const query = {}
  Object.entries(params).forEach(([k, v]) => {
    if (v !== '' && v !== null && v !== undefined && k !== 'page' && k !== 'pageSize') {
      query[k] = v
    }
  })
  router.replace({ path: '/houses', query })
}

/**
 * 处理搜索事件：更新本地筛选状态并重置到第 1 页重新加载
 * @param {Object} newFilters - SearchBar 提交的新筛选条件
 */
function handleSearch(newFilters) {
  Object.assign(filters, newFilters)
  currentPage.value = 1  // 重置到第一页
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.house-list-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
}

.page-content {
  max-width: 1200px;
  margin: 32px auto;
  padding: 0 24px;
  display: flex;
  gap: 24px;
  flex: 1;
  width: 100%;
}

.sidebar {
  width: 280px;
  flex-shrink: 0;
  align-self: flex-start;
  position: sticky;
  top: 80px;
}

.main-content {
  flex: 1;
  min-width: 0;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
  background: #fff;
  padding: 14px 20px;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  transition: box-shadow 0.3s ease;
}

.count-info {
  font-size: 14px;
  color: #606266;
}

.house-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.empty-state {
  padding: 60px 0;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}

.loading-wrap {
  background: #fff;
  padding: 24px;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}

.pagination-wrap {
  margin-top: 32px;
  display: flex;
  justify-content: center;
  background: #fff;
  padding: 16px;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}

@media (max-width: 768px) {
  .page-content {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    position: static;
  }
}
</style>
