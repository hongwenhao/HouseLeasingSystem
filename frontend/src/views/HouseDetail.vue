<template>
  <div class="house-detail-page">
    <NavBar />

    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="10" animated />
    </div>

    <div v-else-if="house" class="detail-content">
      <div class="detail-inner">
        <!-- Image Carousel -->
        <el-carousel height="420px" class="carousel" :autoplay="false">
          <el-carousel-item
            v-for="(img, i) in (house.images && house.images.length > 0 ? house.images : [placeholder])"
            :key="i"
          >
            <img :src="img" :alt="`房屋图片${i+1}`" class="carousel-img" />
          </el-carousel-item>
        </el-carousel>

        <div class="info-section">
          <!-- Title and badges -->
          <div class="title-row">
            <h1 class="house-title">{{ house.title }}</h1>
            <OwnerTypeBadge :ownerType="house.ownerType" />
          </div>

          <div class="price-section">
            <span class="price">¥{{ house.price }}</span>
            <span class="price-unit">元/月</span>
            <el-tag type="info" class="deposit-tag">押金 {{ house.deposit }} 个月</el-tag>
          </div>

          <!-- Key Info -->
          <el-descriptions :column="3" border class="key-info">
            <el-descriptions-item label="城市">{{ house.city }}</el-descriptions-item>
            <el-descriptions-item label="区域">{{ house.district }}</el-descriptions-item>
            <el-descriptions-item label="面积">{{ house.area }}㎡</el-descriptions-item>
            <el-descriptions-item label="户型">{{ house.rooms }}</el-descriptions-item>
            <el-descriptions-item label="楼层">{{ house.floor }}/{{ house.totalFloor }}层</el-descriptions-item>
            <el-descriptions-item label="装修">{{ decorationLabel }}</el-descriptions-item>
            <el-descriptions-item label="地址" :span="3">{{ house.address }}</el-descriptions-item>
            <el-descriptions-item label="可租日期" :span="3">{{ house.availableDate || '随时可住' }}</el-descriptions-item>
          </el-descriptions>

          <!-- Address -->
          <div class="address-row">
            <el-icon><Location /></el-icon>
            <span>{{ house.city }} {{ house.district }} {{ house.address }}</span>
          </div>

          <!-- Fee Table -->
          <div class="section-card">
            <h3 class="card-title">费用说明</h3>
            <FeeTable :fees="house.feeConfig || house.fees || {}" />
          </div>

          <!-- Description -->
          <div class="section-card">
            <h3 class="card-title">房源描述</h3>
            <p class="description">{{ house.description || '暂无描述' }}</p>
          </div>

          <!-- Amenities -->
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

          <!-- Landlord Info -->
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

          <!-- Book Button -->
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
          </div>
        </div>
      </div>
    </div>

    <el-empty v-else description="房源不存在" />

    <!-- Appointment Dialog -->
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
        <el-form-item label="预约日期" prop="appointmentDate">
          <el-date-picker
            v-model="appointForm.appointmentDate"
            type="datetime"
            placeholder="选择预约时间"
            :disabled-date="disablePastDates"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="留言">
          <el-input
            v-model="appointForm.message"
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
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import NavBar from '../components/NavBar.vue'
import Footer from '../components/Footer.vue'
import OwnerTypeBadge from '../components/OwnerTypeBadge.vue'
import FeeTable from '../components/FeeTable.vue'
import { getHouseDetail } from '../api/house.js'
import { createOrder } from '../api/order.js'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const house = ref(null)
const appointmentVisible = ref(false)
const submitting = ref(false)
const appointFormRef = ref(null)
const placeholder = 'https://via.placeholder.com/400x300/409EFF/ffffff?text=房屋图片'

const appointForm = ref({
  appointmentDate: null,
  message: ''
})

const appointRules = {
  appointmentDate: [{ required: true, message: '请选择预约日期', trigger: 'change' }]
}

const decorationLabel = computed(() => {
  const map = { FINE: '精装', SIMPLE: '简装', ROUGH: '毛坯' }
  return map[house.value?.decoration] || house.value?.decoration || '-'
})

function disablePastDates(date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date < today
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getHouseDetail(route.params.id)
    house.value = res
  } catch (e) {
    ElMessage.error('加载房源详情失败')
  } finally {
    loading.value = false
  }
})

function handleBook() {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  appointmentVisible.value = true
}

async function submitAppointment() {
  const valid = await appointFormRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const res = await createOrder({
      houseId: house.value.id,
      appointmentDate: appointForm.value.appointmentDate,
      message: appointForm.value.message
    })
    ElMessage.success('预约成功')
    appointmentVisible.value = false
    if (res && res.id) {
      router.push(`/orders/${res.id}`)
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
  background: #f5f7fa;
}

.loading-wrap {
  max-width: 1000px;
  margin: 40px auto;
  padding: 0 20px;
  width: 100%;
}

.detail-content {
  flex: 1;
  padding: 24px 20px;
}

.detail-inner {
  max-width: 1000px;
  margin: 0 auto;
}

.carousel {
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 24px;
}

.carousel-img {
  width: 100%;
  height: 420px;
  object-fit: cover;
}

.info-section {
  background: #fff;
  border-radius: 12px;
  padding: 28px;
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
  color: #303133;
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
  font-weight: 700;
  color: #f56c6c;
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
}

.book-btn {
  width: 200px;
  font-size: 16px;
}
</style>
