<template>
  <!-- 组件说明：首页视图，包含 Hero 搜索区域、平台数据统计横幅、
       精选推荐房源网格，以及平台优势展示区域。
       页面挂载时同时执行数字动画计数和推荐房源加载。 -->
  <div class="home-page">
    <NavBar />

    <!-- Hero 区域：大图背景 + 搜索栏 -->
    <section class="hero">
      <div class="hero-content">
        <h1 class="hero-title">找到您理想的租房</h1>
        <p class="hero-subtitle">真实房源 · 安全交易 · 快速成交</p>
        <!-- 搜索栏：v-model 双向绑定筛选条件，search 事件触发跳转到房源列表页 -->
        <div class="hero-search">
          <SearchBar v-model="filters" @search="handleSearch" />
        </div>
      </div>
    </section>

    <!-- 平台数据统计横幅 -->
    <section class="stats-banner">
      <div class="stats-inner">
        <div class="stat-item">
          <!-- 动态数字：通过 animateCounter 实现从 0 滚动到目标值的动画效果 -->
          <span class="stat-num">{{ animatedStats.houses }}+</span>
          <span class="stat-label">在租房源</span>
        </div>
        <div class="stat-item">
          <span class="stat-num">{{ animatedStats.users }}+</span>
          <span class="stat-label">注册用户</span>
        </div>
        <div class="stat-item">
          <span class="stat-num">{{ animatedStats.deals }}+</span>
          <span class="stat-label">成交数量</span>
        </div>
        <div class="stat-item">
          <span class="stat-num">{{ animatedStats.cities }}+</span>
          <span class="stat-label">覆盖城市</span>
        </div>
      </div>
    </section>

    <!-- 精选推荐房源区域 -->
    <section class="section">
      <div class="section-inner">
        <div class="section-header">
          <h2 class="section-title">精选推荐房源</h2>
          <router-link to="/houses" class="view-more">查看全部 →</router-link>
        </div>
        <!-- 加载中：骨架屏占位 -->
        <div v-if="loading" class="loading-wrap">
          <el-skeleton :rows="3" animated />
        </div>
        <!-- 推荐房源网格 -->
        <div v-else-if="recommendedHouses.length > 0" class="house-grid">
          <HouseCard
            v-for="house in recommendedHouses"
            :key="house.id"
            :house="house"
          />
        </div>
        <!-- 暂无推荐数据 -->
        <el-empty v-else description="暂无推荐房源" />
      </div>
    </section>

    <!-- 平台优势区域 -->
    <section class="advantages">
      <div class="advantages-inner">
        <h2 class="section-title center">我们的优势</h2>
        <div class="advantage-grid">
          <div class="advantage-item">
            <el-icon class="adv-icon"><Shield /></el-icon>
            <h3>安全保障</h3>
            <p>合同风险智能检测，资金安全有保障，让您租房更放心</p>
          </div>
          <div class="advantage-item">
            <el-icon class="adv-icon"><House /></el-icon>
            <h3>真实房源</h3>
            <p>严格审核房源信息，区分房东类型，所见即所得</p>
          </div>
          <div class="advantage-item">
            <el-icon class="adv-icon"><Lightning /></el-icon>
            <h3>快速成交</h3>
            <p>在线预约看房，电子合同签署，全程数字化服务</p>
          </div>
        </div>
      </div>
    </section>

    <Footer />
  </div>
</template>

<script setup>
// 说明：首页逻辑，负责推荐房源加载和数字动画效果
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import NavBar from '../components/NavBar.vue'
import Footer from '../components/Footer.vue'
import SearchBar from '../components/SearchBar.vue'
import HouseCard from '../components/HouseCard.vue'
import { getRecommended } from '../api/house.js'

const router = useRouter()
const loading = ref(false)
const recommendedHouses = ref([])  // 精选推荐房源列表

// 搜索筛选条件（与 SearchBar 组件 v-model 双向绑定）
const filters = reactive({
  keyword: '',
  city: '',
  district: '',
  minPrice: null,
  maxPrice: null,
  rooms: '',
  ownerType: '',
  decoration: ''
})

// 数字动画状态：存储各统计指标的当前动画显示值
const animatedStats = reactive({
  houses: 0,
  users: 0,
  deals: 0,
  cities: 0
})

/**
 * 数字计数动画：在指定时间内将某个统计字段从 0 平滑增长到目标值
 * @param {string} key      - animatedStats 中的字段名
 * @param {number} target   - 动画目标值
 * @param {number} duration - 动画持续时间（毫秒，默认 1500ms）
 */
function animateCounter(key, target, duration = 1500) {
  const start = Date.now()
  const timer = setInterval(() => {
    const elapsed = Date.now() - start
    const progress = Math.min(elapsed / duration, 1) // 归一化进度 0~1
    animatedStats[key] = Math.floor(progress * target)
    if (progress >= 1) clearInterval(timer) // 动画完成后清除定时器
  }, 16) // 约 60fps 刷新率
}

onMounted(async () => {
  // 启动四个数字的计数动画
  animateCounter('houses', 1280)
  animateCounter('users', 5600)
  animateCounter('deals', 890)
  animateCounter('cities', 32)

  // 加载精选推荐房源
  loading.value = true
  try {
    const res = await getRecommended()
    // 兼容后端返回数组或 { list, total } 两种格式
    recommendedHouses.value = Array.isArray(res) ? res : (res?.list || [])
  } catch (e) {
    ElMessage.error('加载推荐房源失败')
  } finally {
    loading.value = false
  }
})

/**
 * 处理搜索事件：将筛选条件转换为 URL query 参数，跳转到房源列表页
 * @param {Object} searchFilters - SearchBar 组件提交的筛选条件
 */
function handleSearch(searchFilters) {
  const query = {}
  // 过滤掉空值，只保留有效的筛选条件
  Object.entries(searchFilters).forEach(([k, v]) => {
    if (v !== '' && v !== null && v !== undefined) query[k] = v
  })
  router.push({ path: '/houses', query })
}
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.hero {
  background: linear-gradient(135deg, #1a6ebd 0%, #0f4c8c 40%, #1a8a5e 100%);
  padding: 80px 20px;
  color: #fff;
}

.hero-content {
  max-width: 900px;
  margin: 0 auto;
  text-align: center;
}

.hero-title {
  font-size: 42px;
  font-weight: 700;
  margin-bottom: 12px;
  color: #fff;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.hero-subtitle {
  font-size: 18px;
  margin-bottom: 32px;
  opacity: 0.9;
}

.hero-search {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  padding: 4px;
}

.stats-banner {
  background: #409eff;
  padding: 24px 20px;
}

.stats-inner {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  justify-content: space-around;
  flex-wrap: wrap;
  gap: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #fff;
}

.stat-num {
  font-size: 32px;
  font-weight: 700;
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  opacity: 0.9;
  margin-top: 4px;
}

.section {
  padding: 60px 20px;
}

.section-inner {
  max-width: 1200px;
  margin: 0 auto;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;
}

.section-title {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.section-title.center {
  text-align: center;
  margin-bottom: 40px;
}

.view-more {
  color: #409eff;
  text-decoration: none;
  font-size: 14px;
}

.house-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.loading-wrap {
  padding: 40px 0;
}

.advantages {
  background: #f5f7fa;
  padding: 60px 20px;
}

.advantages-inner {
  max-width: 1000px;
  margin: 0 auto;
}

.advantage-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 28px;
}

.advantage-item {
  background: #fff;
  border-radius: 12px;
  padding: 32px 24px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: transform 0.2s;
}

.advantage-item:hover {
  transform: translateY(-4px);
}

.adv-icon {
  font-size: 48px;
  color: #409eff;
  margin-bottom: 16px;
}

.advantage-item h3 {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #303133;
}

.advantage-item p {
  font-size: 14px;
  color: #909399;
  line-height: 1.6;
}
</style>
