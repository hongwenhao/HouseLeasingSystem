<template>
  <div class="house-list-page">
    <NavBar />
    <div class="page-content">
      <div class="sidebar">
        <SearchBar v-model="filters" @search="handleSearch" />
      </div>
      <div class="main-content">
        <div class="toolbar">
          <div class="count-info">
            共找到 <strong>{{ total }}</strong> 套房源
          </div>
          <div class="sort-btns">
            <el-radio-group v-model="sortBy" size="small" @change="fetchData">
              <el-radio-button value="">默认</el-radio-button>
              <el-radio-button value="price_asc">价格升序</el-radio-button>
              <el-radio-button value="price_desc">价格降序</el-radio-button>
              <el-radio-button value="newest">最新</el-radio-button>
            </el-radio-group>
          </div>
        </div>

        <div v-if="loading" class="loading-wrap">
          <el-skeleton :rows="6" animated />
        </div>
        <div v-else-if="houses.length > 0" class="house-grid">
          <HouseCard
            v-for="house in houses"
            :key="house.id"
            :house="house"
          />
        </div>
        <el-empty v-else description="未找到符合条件的房源" class="empty-state" />

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
const houses = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)
const sortBy = ref('')

const filters = reactive({
  keyword: route.query.keyword || '',
  city: route.query.city || '',
  district: route.query.district || '',
  minPrice: route.query.minPrice ? Number(route.query.minPrice) : null,
  maxPrice: route.query.maxPrice ? Number(route.query.maxPrice) : null,
  rooms: route.query.rooms || '',
  ownerType: route.query.ownerType || '',
  decoration: route.query.decoration || ''
})

async function fetchData() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      sortBy: sortBy.value
    }
    Object.entries(filters).forEach(([k, v]) => {
      if (v !== '' && v !== null && v !== undefined) params[k] = v
    })
    updateQuery(params)
    const res = await getHouses(params)
    if (res && res.list !== undefined) {
      houses.value = res.list
      total.value = res.total
    } else if (Array.isArray(res)) {
      houses.value = res
      total.value = res.length
    }
  } catch (e) {
    ElMessage.error('加载房源列表失败')
  } finally {
    loading.value = false
  }
}

function updateQuery(params) {
  const query = {}
  Object.entries(params).forEach(([k, v]) => {
    if (v !== '' && v !== null && v !== undefined && k !== 'page' && k !== 'pageSize') {
      query[k] = v
    }
  })
  router.replace({ path: '/houses', query })
}

function handleSearch(newFilters) {
  Object.assign(filters, newFilters)
  currentPage.value = 1
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.house-list-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.page-content {
  max-width: 1200px;
  margin: 24px auto;
  padding: 0 20px;
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
  padding: 12px 16px;
  border-radius: 8px;
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
  border-radius: 8px;
}

.loading-wrap {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
}

.pagination-wrap {
  margin-top: 24px;
  display: flex;
  justify-content: center;
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
