<template>
  <!-- 组件说明：首页视图，包含 Hero 搜索区域、精选推荐房源网格、
       平台数据统计横幅，以及平台优势展示区域。
       精选推荐房源置于统计横幅之前，优先展示核心租房内容。
       页面挂载时同时执行数字动画计数和推荐房源加载。 -->
  <div class="home-page">
    <NavBar />

    <!-- Hero 区域：大图背景 + 紧凑搜索栏 -->
    <section class="hero">
      <div class="hero-content">
        <h1 class="hero-title">找到您理想的租房</h1>
        <p class="hero-subtitle">真实房源 · 安全交易 · 快速成交</p>
        <!-- 单行紧凑搜索栏 -->
        <div class="hero-search">
          <SearchBar v-model="filters" :compact="true" @search="handleSearch" />
        </div>
      </div>
    </section>

    <!-- 精选推荐房源区域（紧随 Hero，突出展示） -->
    <section class="featured-section">
      <div class="featured-inner">
        <div class="featured-header">
          <div class="featured-title-wrap">
            <span class="featured-badge">🔥 精选推荐</span>
            <h2 class="featured-title">为您推荐的好房源</h2>
            <p class="featured-subtitle">严格筛选，品质保证，快速找到理想居所</p>
          </div>
          <router-link to="/houses" class="view-more">查看全部房源 →</router-link>
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

    <!-- 平台优势区域 -->
    <section class="advantages">
      <div class="advantages-inner">
        <h2 class="section-title center">我们的优势</h2>
        <div class="advantage-grid">
          <div class="advantage-item">
            <el-icon class="adv-icon"><Shield /></el-icon>
            <h3 class="adv-title-with-icon">
              <!-- 补充标题级安全图标：当主图标在个别环境渲染异常时，仍有清晰视觉提示 -->
              <el-icon class="adv-title-icon"><CircleCheckFilled /></el-icon>
              <span>安全保障</span>
            </h3>
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
import { getRecommended, getHomeStats } from '../api/house.js'

const router = useRouter()
const loading = ref(false)
const recommendedHouses = ref([])  // 精选推荐房源列表

// 搜索筛选条件（与 SearchBar 组件 v-model 双向绑定）
const filters = reactive({
  keyword: '',
  province: '',
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

const statsTarget = reactive({
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
  // 并行加载：首页统计与精选推荐房源
  loading.value = true
  try {
    const [statsRes, recommendedRes] = await Promise.all([
      getHomeStats(),
      getRecommended()
    ])

    statsTarget.houses = Number(statsRes?.houses || 0)
    statsTarget.users = Number(statsRes?.users || 0)
    statsTarget.deals = Number(statsRes?.deals || 0)
    statsTarget.cities = Number(statsRes?.cities || 0)

    // 启动四个数字的计数动画
    animateCounter('houses', statsTarget.houses)
    animateCounter('users', statsTarget.users)
    animateCounter('deals', statsTarget.deals)
    animateCounter('cities', statsTarget.cities)

    // 兼容后端返回数组或 { list, total } 两种格式
    recommendedHouses.value = Array.isArray(recommendedRes) ? recommendedRes : (recommendedRes?.list || [])
  } catch (e) {
    ElMessage.error('加载首页数据失败')
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
  // 过滤掉空值和前端专用的 province 字段，只保留有效的筛选条件
  Object.entries(searchFilters).forEach(([k, v]) => {
    if (k !== 'province' && v !== '' && v !== null && v !== undefined) query[k] = v
  })
  router.push({ path: '/houses', query })
}
</script>

<style scoped>
/* ===== 首页布局 ===== */
.home-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* ===== Hero 主视觉区域：大面积渐变 + 装饰光圈 ===== */
.hero {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 40%, #f093fb 100%);
  padding: 60px 20px 50px;
  color: #fff;
  position: relative;
  overflow: hidden;
}

/* Hero 背景装饰光圈 */
.hero::before {
  content: '';
  position: absolute;
  top: -30%;
  right: -15%;
  width: 600px;
  height: 600px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.06);
}

.hero::after {
  content: '';
  position: absolute;
  bottom: -20%;
  left: -10%;
  width: 500px;
  height: 500px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.04);
}

.hero-content {
  max-width: 900px;
  margin: 0 auto;
  text-align: center;
  position: relative;
  z-index: 1;
}

/* Hero 大标题：白色 + 文字阴影增强可读性 */
.hero-title {
  font-size: 42px;
  font-weight: 800;
  margin-bottom: 12px;
  color: #fff;
  text-shadow: 0 2px 20px rgba(0, 0, 0, 0.15);
  letter-spacing: 2px;
  animation: fadeInDown 0.8s ease-out;
}

/* Hero 副标题 */
.hero-subtitle {
  font-size: 17px;
  margin-bottom: 28px;
  opacity: 0.92;
  font-weight: 300;
  letter-spacing: 1px;
  animation: fadeInDown 0.8s ease-out 0.2s both;
}

/* 搜索栏容器：毛玻璃白色背景 */
.hero-search {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 16px;
  padding: 6px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  animation: fadeInUp 0.8s ease-out 0.4s both;
}

/* 入场动画 */
@keyframes fadeInDown {
  from { opacity: 0; transform: translateY(-20px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ===== 数据统计横幅 ===== */
.stats-banner {
  background: linear-gradient(135deg, #1a1a2e, #16213e);
  padding: 32px 20px;
}

.stats-inner {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  justify-content: space-around;
  flex-wrap: wrap;
  gap: 20px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #fff;
}

/* 统计数字：大号 + 渐变色文字 */
.stat-num {
  font-size: 36px;
  font-weight: 800;
  line-height: 1;
  background: linear-gradient(135deg, #667eea, #f093fb);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-label {
  font-size: 14px;
  opacity: 0.7;
  margin-top: 6px;
  letter-spacing: 0.5px;
}

/* ===== 精选推荐房源区域（Hero 正下方，突出展示） ===== */
.featured-section {
  background: linear-gradient(180deg, #faf7ff 0%, #ffffff 100%);
  padding: 56px 20px 64px;
  border-top: 3px solid rgba(102, 126, 234, 0.15);
}

.featured-inner {
  max-width: 1200px;
  margin: 0 auto;
}

/* 区域头部：左侧标题组 + 右侧"查看全部"链接 */
.featured-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 36px;
  flex-wrap: wrap;
  gap: 16px;
}

.featured-title-wrap {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 橙红渐变小徽章 */
.featured-badge {
  display: inline-flex;
  align-items: center;
  background: linear-gradient(135deg, #ff9800, #ff5722);
  color: #fff;
  padding: 4px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  width: fit-content;
  box-shadow: 0 2px 8px rgba(255, 87, 34, 0.35);
}

/* 大标题 */
.featured-title {
  font-size: 30px;
  font-weight: 800;
  color: #1a1a2e;
  line-height: 1.2;
}

/* 副标题说明文字 */
.featured-subtitle {
  font-size: 14px;
  color: #909399;
}

/* "查看全部"链接 */
.view-more {
  color: #667eea;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: color 0.2s;
  display: flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

.view-more:hover {
  color: #764ba2;
}

/* 房源卡片网格 */
.house-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}

.loading-wrap {
  padding: 40px 0;
}

/* ===== 平台优势区域 ===== */
.advantages {
  background: linear-gradient(180deg, #f8f9fe 0%, #f0f2f5 100%);
  padding: 64px 20px;
}

/* 优势区域标题 */
.section-title {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a2e;
}

.section-title.center {
  text-align: center;
  margin-bottom: 48px;
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

/* 优势卡片：悬停上浮 + 渐变边框指示 */
.advantage-item {
  background: #fff;
  border-radius: 16px;
  padding: 36px 28px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.advantage-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #667eea, #764ba2);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.advantage-item:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);
}

.advantage-item:hover::before {
  opacity: 1;
}

/* 优势图标：浅色圆形背景 */
.adv-icon {
  font-size: 40px;
  color: #667eea;
  margin-bottom: 20px;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.advantage-item h3 {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #1a1a2e;
}

/* “安全保障”标题图标样式：作为主图标的冗余补充，提升可识别性 */
.adv-title-with-icon {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.adv-title-icon {
  font-size: 16px;
  color: #67c23a;
}

.advantage-item p {
  font-size: 14px;
  color: #909399;
  line-height: 1.7;
}
</style>
