<template>
  <!-- 组件说明：房源详情页，展示单套房源的完整信息，包括：
       图片轮播、标题与房东类型角标、价格和押金、关键参数（户型/面积/楼层/装修等）、
       地址、五项费用表、房源描述、配套设施、房东信息以及预约看房按钮。
       点击预约看房会弹出预约对话框，未登录用户会被重定向到登录页。 -->
  <div class="house-detail-page">
    <NavBar />

    <!-- 加载状态：骨架屏占位 -->
    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="10" animated />
    </div>

    <!-- 房源详情主体 -->
    <div v-else-if="house" class="detail-content">
      <div class="detail-inner">
        <!-- 图片轮播区域：没有图片时使用占位图 -->
        <el-carousel height="420px" class="carousel" :autoplay="false">
          <el-carousel-item
            v-for="(img, i) in (house.images && house.images.length > 0 ? house.images : [placeholder])"
            :key="i"
          >
            <img :src="img" :alt="`房屋图片${i+1}`" class="carousel-img" />
          </el-carousel-item>
        </el-carousel>

        <div class="info-section">
          <!-- 标题行：房源标题 + 房东类型角标 -->
          <div class="title-row">
            <h1 class="house-title">{{ house.title }}</h1>
            <OwnerTypeBadge :ownerType="house.ownerType" />
          </div>

          <!-- 价格区域 -->
          <div class="price-section">
            <span class="price">¥{{ house.price }}</span>
            <span class="price-unit">元/月</span>
            <el-tag type="info" class="deposit-tag">押金 {{ house.deposit }} 个月</el-tag>
          </div>

          <!-- 关键信息描述列表 -->
          <el-descriptions :column="3" border class="key-info">
            <el-descriptions-item label="城市">{{ displayCity }}</el-descriptions-item>
            <el-descriptions-item label="区域">{{ displayDistrict }}</el-descriptions-item>
            <el-descriptions-item label="面积">{{ house.area }}㎡</el-descriptions-item>
            <el-descriptions-item label="户型">{{ house.rooms }}室{{ house.halls }}厅{{ house.bathrooms }}卫</el-descriptions-item>
            <el-descriptions-item label="楼层">{{ house.floor }}/{{ house.totalFloor }}层</el-descriptions-item>
            <el-descriptions-item label="装修">{{ decorationLabel }}</el-descriptions-item>
            <el-descriptions-item label="地址" :span="3">{{ house.address }}</el-descriptions-item>
            <el-descriptions-item label="可租日期" :span="3">{{ house.availableDate || '随时可住' }}</el-descriptions-item>
          </el-descriptions>

          <!-- 详细地址行（含地图定位图标） -->
          <div class="address-row">
            <el-icon><Location /></el-icon>
            <span>{{ displayCity }} {{ displayDistrict }} {{ house.address }}</span>
          </div>

          <!-- 五项费用说明 -->
          <div class="section-card">
            <h3 class="card-title">费用说明</h3>
            <FeeTable :fees="house.feeConfig || house.fees || {}" />
          </div>

          <!-- 房源描述 -->
          <div class="section-card">
            <h3 class="card-title">房源描述</h3>
            <p class="description">{{ house.description || '暂无描述' }}</p>
          </div>

          <!-- 配套设施标签（无设施时不显示该区块） -->
          <div class="section-card" v-if="house.amenities && house.amenities.length > 0">
            <h3 class="card-title">配套设施</h3>
            <div class="amenities">
              <el-tag
                v-for="item in house.amenities"
                :key="item"
                type="info"
                class="amenity-tag"
              >{{ item }}</el-tag>
            </div>
          </div>

          <!-- 房东信息卡片 -->
          <div class="section-card landlord-card">
            <h3 class="card-title">房东信息</h3>
            <div class="landlord-info" v-if="house.landlord">
              <el-avatar :size="48" :icon="UserFilled" :src="house.landlord.avatarUrl" />
              <div class="landlord-detail">
                <span class="landlord-name">{{ house.landlord.username }}</span>
                <el-tag size="small">信用分 {{ house.landlord.creditScore || 100 }}</el-tag>
              </div>
            </div>
            <p v-else class="no-info">暂无房东信息</p>
          </div>

          <!-- 预约看房按钮 -->
          <div class="action-section">
            <el-button
              type="primary"
              size="large"
              @click="handleBook"
              class="book-btn"
            >
              <el-icon><Calendar /></el-icon>
              预约看房
            </el-button>
            <el-button
              v-if="isTenant"
              size="large"
              :type="collected ? 'success' : 'warning'"
              :plain="!collected"
              :loading="collecting"
              @click.stop="handleCollect"
            >
              <el-icon><Star /></el-icon>
              {{ collected ? '已收藏' : '收藏' }}
            </el-button>
            <el-button
              v-else
              size="large"
              disabled
              plain
              aria-label="仅租客可以收藏房源"
              title="仅租客可以收藏房源"
            >
              <el-icon><Star /></el-icon>
              仅租客可收藏
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 房源不存在时的空状态 -->
    <el-empty v-else description="房源不存在" />

    <!-- 预约看房对话框 -->
    <el-dialog
      v-model="appointmentVisible"
      title="预约看房"
      width="500px"
    >
      <el-form
        ref="appointFormRef"
        :model="appointForm"
        :rules="appointRules"
        label-width="100px"
      >
        <!-- 预约日期时间选择器（禁用过去日期） -->
        <el-form-item label="预约日期" prop="appointmentTime">
          <el-date-picker
            v-model="appointForm.appointmentTime"
            type="datetime"
            placeholder="选择预约时间"
            :disabled-date="disablePastDates"
            style="width: 100%"
          />
        </el-form-item>
        <!-- 留言（可选） -->
        <el-form-item label="留言">
          <el-input
            v-model="appointForm.remark"
            type="textarea"
            :rows="3"
            placeholder="可留言说明看房目的或联系方式"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="appointmentVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAppointment">
          提交预约
        </el-button>
      </template>
    </el-dialog>

    <Footer />
  </div>
</template>

<script setup>
// 说明：房源详情页逻辑，加载房源完整信息并处理预约看房流程
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UserFilled, Star } from '@element-plus/icons-vue'
import NavBar from '../components/NavBar.vue'
import Footer from '../components/Footer.vue'
import OwnerTypeBadge from '../components/OwnerTypeBadge.vue'
import FeeTable from '../components/FeeTable.vue'
import { collectHouse, getHouseDetail, getMyCollections } from '../api/house.js'
import { createOrder } from '../api/order.js'
import { useUserStore } from '../stores/user.js'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const house = ref(null)             // 当前房源详情数据
const appointmentVisible = ref(false) // 控制预约对话框显隐
const submitting = ref(false)        // 提交预约按钮 loading 状态
const collecting = ref(false)        // 收藏按钮 loading 状态
const collected = ref(false)         // 是否已收藏
const appointFormRef = ref(null)
const placeholder = 'https://via.placeholder.com/400x300/409EFF/ffffff?text=房屋图片'
const GROUPING_CITY_LABELS = ['市辖区', '省直辖县级行政区划', '县']  // 行政区划中的占位分组名称
const isTenant = computed(() => userStore.userInfo.role === 'TENANT')

// 预约表单数据
const appointForm = ref({
  appointmentTime: null,  // 预约看房时间（对应后端 OrderCreateRequest.appointmentTime）
  remark: ''              // 留言备注（对应后端 OrderCreateRequest.remark）
})

const appointRules = {
  appointmentTime: [{ required: true, message: '请选择预约日期', trigger: 'change' }]
}

/**
 * 直辖市/省直辖县级等占位城市用省份兜底展示，避免出现“市辖区”字样
 */
const displayCity = computed(() => {
  if (!house.value) return ''
  const city = house.value.city
  if (!city || GROUPING_CITY_LABELS.includes(city)) {
    return house.value.province || city || '-'
  }
  return city
})

/** 区县同样过滤分组占位名称 */
const displayDistrict = computed(() => {
  if (!house.value) return ''
  const district = house.value.district
  if (!district || GROUPING_CITY_LABELS.includes(district)) {
    return ''
  }
  return district
})

/**
 * 计算装修情况的中文标签
 * FINE → 精装，SIMPLE → 简装，MEDIUM → 中等装修，ROUGH → 毛坯，LUXURY → 豪装
 */
const decorationLabel = computed(() => {
  const map = {
    FINE: '精装',
    SIMPLE: '简装',
    MEDIUM: '中等装修',
    ROUGH: '毛坯',
    LUXURY: '豪装'
  }
  return map[house.value?.decoration] || house.value?.decoration || '-'
})

/**
 * 日期选择器禁用过去日期（预约时间必须是今天或之后）
 * @param {Date} date - 待判断的日期
 */
function disablePastDates(date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date < today
}

/** 查询当前房源是否已被当前用户收藏 */
async function checkCollected() {
  if (!userStore.isLoggedIn || !isTenant.value) return
  try {
    const res = await getMyCollections({ page: 1, pageSize: 100 })
    const list = Array.isArray(res) ? res : (res?.records || res?.list || [])
    collected.value = list.some((item) => String(item.id) === String(route.params.id))
  } catch (e) { /* ignore */ }
}

onMounted(async () => {
  loading.value = true
  try {
    // 从路由参数中获取房源 ID 并加载详情
    const res = await getHouseDetail(route.params.id)
    house.value = res
    await checkCollected()
  } catch (e) {
    ElMessage.error('加载房源详情失败')
  } finally {
    loading.value = false
  }
})

/**
 * 处理预约看房按钮点击
 * 未登录用户重定向到登录页，并携带 redirect 参数以便登录后返回
 */
function handleBook() {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  appointmentVisible.value = true
}

/** 收藏房源 */
async function handleCollect() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  // 房东/管理员无需收藏，直接给出提示
  if (!isTenant.value) {
    ElMessage.warning('仅租客可以收藏房源')
    return
  }
  collecting.value = true
  try {
    await collectHouse(route.params.id)
    collected.value = true
    ElMessage.success('已收藏该房源')
  } catch (e) {
    ElMessage.error(e.message || '收藏失败')
  } finally {
    collecting.value = false
  }
}

/**
 * 提交预约申请
 * 调用创建订单接口，成功后跳转到订单详情页
 */
async function submitAppointment() {
  const valid = await appointFormRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const res = await createOrder({
      houseId: house.value.id,
      appointmentTime: appointForm.value.appointmentTime, // 后端 OrderCreateRequest.appointmentTime
      remark: appointForm.value.remark                   // 后端 OrderCreateRequest.remark
    })
    ElMessage.success('预约成功')
    appointmentVisible.value = false
    if (res && res.id) {
      router.push(`/orders/${res.id}`)  // 跳转到新创建的订单详情页
    }
  } catch (e) {
    ElMessage.error(e.message || '预约失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.house-detail-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
}

.loading-wrap {
  max-width: 1000px;
  margin: 40px auto;
  padding: 0 20px;
  width: 100%;
}

.detail-content {
  flex: 1;
  padding: 32px 20px;
}

.detail-inner {
  max-width: 1000px;
  margin: 0 auto;
}

.carousel {
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}

.carousel-img {
  width: 100%;
  height: 420px;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.info-section {
  background: #fff;
  border-radius: 16px;
  padding: 28px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.house-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  flex: 1;
}

.price-section {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 20px;
}

.price {
  font-size: 36px;
  font-weight: 800;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.price-unit {
  font-size: 16px;
  color: #909399;
}

.deposit-tag {
  margin-left: 8px;
}

.key-info {
  margin-bottom: 20px;
}

.address-row {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #606266;
  font-size: 14px;
  margin-bottom: 20px;
}

.section-card {
  border-top: 1px solid #ebeef5;
  padding: 20px 0;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.description {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  white-space: pre-wrap;
}

.amenities {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.amenity-tag {
  margin: 0;
}

.landlord-card .landlord-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.landlord-detail {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.landlord-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.no-info {
  color: #909399;
  font-size: 14px;
}

.action-section {
  padding-top: 20px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.book-btn {
  width: 200px;
  font-size: 16px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
  border-radius: 8px;
  color: #fff;
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.book-btn:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}
</style>
