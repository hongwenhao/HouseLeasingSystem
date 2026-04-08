<template>
  <!-- 组件说明：房源详情页，展示单套房源的完整信息。
       布局：左列（图片轮播 + 详情内容）+ 右列（粘性预约卡，含价格、关键参数和操作按钮）。
       未登录用户点击预约/收藏会被重定向到登录页。 -->
  <div class="house-detail-page">
    <NavBar />

    <!-- 加载状态：骨架屏占位 -->
    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="12" animated />
    </div>

    <!-- 房源详情主体 -->
    <div v-else-if="house" class="detail-content">
      <div class="detail-inner">
        <!-- ========== 双栏布局：左列（轮播+详情）+ 右列（预约卡） ========== -->
        <div class="main-layout">

          <!-- ── 左列：图片轮播 + 详情内容 ── -->
          <div class="left-col">
            <!-- 图片轮播 -->
            <div class="carousel-wrap">
              <el-carousel height="380px" class="carousel" :autoplay="false" indicator-position="outside">
                <el-carousel-item
                  v-for="(img, i) in normalizedImages"
                  :key="i"
                >
                  <img :src="img" :alt="`房屋图片${i+1}`" class="carousel-img" />
                </el-carousel-item>
              </el-carousel>
            </div>

            <!-- 房源描述 -->
            <div class="section-card">
              <h3 class="card-title">
                <el-icon class="title-icon"><Document /></el-icon>房源描述
              </h3>
              <p class="description">{{ house.description || '暂无描述' }}</p>
            </div>

            <!-- 五项费用说明 -->
            <div class="section-card">
              <h3 class="card-title">
                <el-icon class="title-icon"><Money /></el-icon>费用说明
              </h3>
              <FeeTable :fees="feesConfig" />
            </div>

            <!-- 配套设施标签（无设施时不显示该区块） -->
            <div class="section-card" v-if="house.amenities && house.amenities.length > 0">
              <h3 class="card-title">
                <el-icon class="title-icon"><Grid /></el-icon>配套设施
              </h3>
              <div class="amenities">
                <el-tag
                  v-for="item in house.amenities"
                  :key="item"
                  type="info"
                  effect="plain"
                  class="amenity-tag"
                >{{ item }}</el-tag>
              </div>
            </div>
          </div>

          <!-- ── 右列：粘性信息卡 ── -->
          <div class="right-col">
            <div class="booking-card">
              <!-- 房东类型角标 -->
              <div class="badge-row">
                <OwnerTypeBadge :ownerType="house.ownerType" />
                <el-tag v-if="house.status === 'ONLINE'" type="success" size="small" effect="plain">在线</el-tag>
              </div>

              <!-- 房源标题 -->
              <h1 class="house-title">{{ house.title }}</h1>

              <!-- 价格横幅（渐变背景） -->
              <div class="price-banner">
                <div class="price-main">
                  <span class="price-symbol">¥</span>
                  <span class="price-value">{{ house.price }}</span>
                  <span class="price-unit">元/月</span>
                </div>
                <!-- 押金：显示月数及对应的实际金额（押金月数 × 月租金） -->
                <div class="price-sub">
                  押金 <strong>{{ house.deposit }}</strong> 个月
                  <span v-if="depositAmount > 0">（¥{{ depositAmount.toFixed(2) }}）</span>
                </div>
              </div>

              <!-- 关键参数标签 -->
              <div class="specs-row">
                <el-tag size="small" type="info" effect="plain">
                  {{ displayCity }}{{ displayDistrict ? ' · ' + displayDistrict : '' }}
                </el-tag>
                <el-tag size="small" type="info" effect="plain">
                  {{ house.rooms }}室{{ house.halls }}厅{{ house.bathrooms }}卫
                </el-tag>
                <el-tag size="small" type="info" effect="plain">{{ house.area }}㎡</el-tag>
                <el-tag size="small" type="info" effect="plain">
                  {{ house.floor }}/{{ house.totalFloor }}层
                </el-tag>
                <el-tag size="small" type="info" effect="plain">{{ houseTypeLabel }}</el-tag>
                <el-tag size="small" type="info" effect="plain">{{ decorationLabel }}</el-tag>
              </div>

              <el-divider class="card-divider" />

              <!-- 可租日期 -->
              <div class="available-row">
                <el-icon class="available-icon"><Calendar /></el-icon>
                <span class="available-label">可入住：</span>
                <span class="available-value">{{ house.availableDate || '随时可住' }}</span>
              </div>

              <!-- 地址 -->
              <div class="address-row">
                <el-icon class="addr-icon"><Location /></el-icon>
                <span class="addr-text">{{ displayCity }} {{ displayDistrict }} {{ house.address }}</span>
              </div>

              <!-- 操作按钮 -->
              <div v-if="shouldShowBookingActions" class="action-btns">
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
                  :type="collected ? 'success' : 'default'"
                  :plain="!collected"
                  :loading="collecting"
                  @click.stop="handleCollect"
                  class="collect-btn"
                >
                  <el-icon><Star /></el-icon>
                  {{ collected ? '已收藏' : '收藏' }}
                </el-button>
                <el-button
                  v-else
                  size="large"
                  disabled
                  plain
                  class="collect-btn"
                  aria-label="仅租客可以收藏房源"
                  title="仅租客可以收藏房源"
                >
                  <el-icon><Star /></el-icon>
                  收藏
                </el-button>
              </div>

              <!-- 房东信息（迷你版） -->
              <div v-if="house.landlord">
                <el-divider class="card-divider" />
                <div class="landlord-row">
                  <el-avatar :size="40" :icon="UserFilled" :src="house.landlord.avatarUrl" />
                  <div class="landlord-info">
                    <span class="landlord-name">{{ house.landlord.username }}</span>
                    <el-tag size="small" type="warning" effect="plain">
                      信用分 {{ house.landlord.creditScore || 100 }}
                    </el-tag>
                  </div>
                </div>
              </div>
            </div>
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
import { UserFilled, Star, Document, Money, Grid } from '@element-plus/icons-vue'
import NavBar from '../components/NavBar.vue'
import Footer from '../components/Footer.vue'
import OwnerTypeBadge from '../components/OwnerTypeBadge.vue'
import FeeTable from '../components/FeeTable.vue'
import { collectHouse, getHouseDetail, getMyCollections, uncollectHouse } from '../api/house.js'
import { createOrder } from '../api/order.js'
import { useUserStore } from '../stores/user.js'
import { normalizeHouseImages } from '../utils/houseImages.js'

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
const HOUSE_TYPE_LABEL_MAP = {
  APARTMENT: '公寓',
  HOUSE: '住宅',
  ROOM: '单间',
  VILLA: '别墅'
}
const isTenant = computed(() => userStore.userInfo.role === 'TENANT')
const isLandlord = computed(() => userStore.userInfo.role === 'LANDLORD')
const isAdmin = computed(() => userStore.userInfo.role === 'ADMIN')
// 仅租客或未登录访客显示预约/收藏入口；房东与管理员都不显示，满足“管理员和房东都不能预约/收藏”。
const shouldShowBookingActions = computed(() => !isLandlord.value && !isAdmin.value)
const normalizedImages = computed(() => {
  const images = normalizeHouseImages(house.value?.images)
  return images.length > 0 ? images : [placeholder]
})

/**
 * 计算实际押金金额（元）
 * 后端 houses.deposit 存储押金月数（如 1 表示押一个月租金），
 * 实际押金 = depositMonths × monthlyPrice，供详情页展示使用
 */
const depositAmount = computed(() => {
  if (!house.value || !house.value.deposit || !house.value.price) return 0
  // 返回数值类型，模板中用 toFixed(2) 格式化显示
  return Number(house.value.deposit) * Number(house.value.price)
})

/**
 * 将 house 的扁平费用字段转换为 FeeTable 组件所需的嵌套结构
 * House 实体：house.waterFee / house.waterFeeType → FeeTable 期望：{ waterFee: { type, amount } }
 */
const feesConfig = computed(() => {
  if (!house.value) return {}
  return {
    waterFee: { type: house.value.waterFeeType, amount: house.value.waterFee },
    electricFee: { type: house.value.electricFeeType, amount: house.value.electricFee },
    gasFee: { type: house.value.gasFeeType, amount: house.value.gasFee },
    propertyFee: { type: house.value.propertyFeeType, amount: house.value.propertyFee },
    internetFee: { type: house.value.internetFeeType, amount: house.value.internetFee }
  }
})

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
 * 计算房源类型的中文标签（用于“关键参数标签”区域展示）
 * 说明：
 * 1) 后端返回的 houseType 为英文枚举值，前端需要转换成用户可读的中文文案；
 * 2) 该标签与“装修情况”标签同级并排展示，便于用户快速识别房源属性；
 * 3) 若后端新增了未收录的枚举值，优先原样展示该值，避免页面出现空白；
 * 4) 当 houseType 缺失时，统一返回 "-" 作为兜底占位。
 * 枚举映射：
 * APARTMENT → 公寓，HOUSE → 住宅，ROOM → 单间，VILLA → 别墅
 */
const houseTypeLabel = computed(() => {
  const houseType = house.value?.houseType
  if (!houseType) return '-'
  // map 中存在时返回中文标签；不存在时回退为后端原始值，避免新枚举上线后页面空白。
  return HOUSE_TYPE_LABEL_MAP[houseType] ?? houseType
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
    const res = await getMyCollections({ page: 1, size: 100 })
    const list = Array.isArray(res) ? res : (res?.records || res?.list || [])
    collected.value = list.some((item) => String(item.id) === String(route.params.id))
  } catch (e) { /* ignore */ }
}

onMounted(async () => {
  loading.value = true
  try {
    // 从路由参数中获取房源 ID 并加载详情
    const res = await getHouseDetail(route.params.id)
    house.value = {
      ...res,
      // 将图片 JSON 字符串/数组标准化为 URL 数组
      images: normalizeHouseImages(res?.images),
      // 将后端逗号分隔的 tags 字符串解析为配套设施数组，供模板 v-for 渲染
      // 例如 "洗衣机,空调,WiFi" → ['洗衣机', '空调', 'WiFi']
      amenities: res?.tags ? res.tags.split(',').filter(tag => tag.trim() !== '') : []
    }
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
  // 防御式校验：即使被非常规方式触发（如控制台调用），也阻止房东/管理员预约
  if (!isTenant.value) {
    ElMessage.warning('仅租客可以预约看房')
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
  const isUncollecting = collected.value
  try {
    if (isUncollecting) {
      await uncollectHouse(route.params.id)
      collected.value = false
      ElMessage.success('已取消收藏')
    } else {
      await collectHouse(route.params.id)
      collected.value = true
      ElMessage.success('已收藏该房源')
    }
  } catch (e) {
    const msg = isUncollecting ? '取消收藏失败' : '收藏失败'
    ElMessage.error(e.message || msg)
  } finally {
    collecting.value = false
  }
}

/**
 * 提交预约申请
 * 调用创建订单接口，成功后跳转到订单详情页
 */
async function submitAppointment() {
  // 预约提交前主动拉取一次最新用户资料，避免本地缓存过期导致实名认证状态误判
  try {
    await userStore.fetchProfile()
  } catch (e) {
    console.warn('预约前刷新用户资料失败，将使用缓存数据验证实名认证状态（可能存在短暂误判）:', e)
  }
  if (!userStore.userInfo?.isRealNameAuth) {
    ElMessage.warning('请先在个人中心完成实名认证后再预约看房')
    return
  }
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
/* ===== 页面容器 ===== */
.house-detail-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
}

.loading-wrap {
  max-width: 1100px;
  margin: 40px auto;
  padding: 0 24px;
  width: 100%;
}

.detail-content {
  flex: 1;
  padding: 28px 24px 48px;
}

.detail-inner {
  max-width: 1100px;
  margin: 0 auto;
}

/* ===== 双栏主布局 ===== */
.main-layout {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 24px;
  align-items: start;
  margin-bottom: 24px;
}

/* ── 左列：图片轮播 + 详情内容 ── */
.left-col {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.carousel-wrap {
  background: #eef2ff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.06);
  padding: 12px 12px 0;
}

.carousel {
  border-radius: 12px;
  overflow: hidden;
}

.carousel-img {
  width: 100%;
  height: 380px;
  object-fit: cover;
  display: block;
}

/* ── 右列：粘性信息卡 ── */
.right-col {
  min-width: 0;
}

.booking-card {
  position: sticky;
  top: 20px;
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

/* 角标行 */
.badge-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

/* 标题 */
.house-title {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
  line-height: 1.4;
  margin-bottom: 16px;
  word-break: break-all;
}

/* 价格横幅 */
.price-banner {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 16px;
  color: #fff;
}

.price-main {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 6px;
}

.price-symbol {
  font-size: 20px;
  font-weight: 700;
  opacity: 0.9;
}

.price-value {
  font-size: 36px;
  font-weight: 800;
  line-height: 1;
}

.price-unit {
  font-size: 14px;
  opacity: 0.85;
}

.price-sub {
  font-size: 13px;
  opacity: 0.85;
}

/* 关键参数标签 */
.specs-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 4px;
}

/* 分隔线 */
.card-divider {
  margin: 16px 0;
}

/* 可租日期 */
.available-row {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #606266;
  font-size: 14px;
  margin-bottom: 10px;
}

.available-icon {
  color: #667eea;
  flex-shrink: 0;
}

.available-label {
  color: #909399;
}

.available-value {
  color: #303133;
  font-weight: 500;
}

/* 地址行 */
.address-row {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  color: #606266;
  font-size: 13px;
  margin-bottom: 16px;
  line-height: 1.5;
}

.addr-icon {
  color: #f56c6c;
  flex-shrink: 0;
  margin-top: 2px;
}

.addr-text {
  flex: 1;
}

/* 操作按钮 */
.action-btns {
  display: flex;
  gap: 10px;
  margin-bottom: 4px;
}

.book-btn {
  flex: 1;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 15px;
  transition: opacity 0.3s, transform 0.2s;
}

.book-btn:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.collect-btn {
  flex-shrink: 0;
}

/* 房东迷你信息 */
.landlord-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.landlord-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.landlord-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

/* ===== 各内容分区卡片 ===== */
.section-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-icon {
  font-size: 18px;
  color: #667eea;
}

.description {
  font-size: 14px;
  color: #606266;
  line-height: 1.9;
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

/* ===== 响应式适配 ===== */
@media (max-width: 768px) {
  .main-layout {
    grid-template-columns: 1fr;
  }

  .booking-card {
    position: static;
  }

  .carousel-img {
    height: 240px;
  }
}
</style>
