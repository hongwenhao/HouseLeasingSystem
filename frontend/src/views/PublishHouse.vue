<template>
  <div class="publish-house-page">
    <NavBar />
    <div class="page-content">
      <div class="page-inner">
        <h2 class="page-title">{{ isEdit ? '编辑房源' : '发布房源' }}</h2>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="120px"
          class="publish-form"
        >
          <!-- Basic Info -->
          <el-card class="form-card">
            <template #header>基本信息</template>
            <el-form-item label="房源标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入房源标题" maxlength="60" show-word-limit />
            </el-form-item>
            <el-form-item label="房源描述" prop="description">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="4"
                placeholder="请描述房源特点、周边环境等信息"
                maxlength="1000"
                show-word-limit
              />
            </el-form-item>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="城市" prop="city">
                  <el-input v-model="form.city" placeholder="如：北京" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="区域" prop="district">
                  <el-input v-model="form.district" placeholder="如：朝阳区" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="详细地址" prop="address">
              <el-input v-model="form.address" placeholder="请输入详细地址" />
            </el-form-item>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="经度">
                  <el-input v-model.number="form.longitude" type="number" placeholder="可选" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="纬度">
                  <el-input v-model.number="form.latitude" type="number" placeholder="可选" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-card>

          <!-- House Info -->
          <el-card class="form-card">
            <template #header>房屋信息</template>
            <el-row :gutter="20">
              <el-col :span="8">
                <el-form-item label="面积(㎡)" prop="area">
                  <el-input-number v-model="form.area" :min="1" :max="9999" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="户型" prop="rooms">
                  <div style="display:flex;gap:4px;align-items:center">
                    <el-input-number v-model="form.rooms" :min="1" :max="20" controls-position="right" style="width:80px" />
                    <span>室</span>
                    <el-input-number v-model="form.halls" :min="0" :max="10" controls-position="right" style="width:80px" />
                    <span>厅</span>
                    <el-input-number v-model="form.bathrooms" :min="0" :max="10" controls-position="right" style="width:80px" />
                    <span>卫</span>
                  </div>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="房屋类型">
                  <el-select v-model="form.houseType" style="width:100%">
                    <el-option label="公寓" value="APARTMENT" />
                    <el-option label="住宅" value="HOUSE" />
                    <el-option label="单间" value="ROOM" />
                    <el-option label="别墅" value="VILLA" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="8">
                <el-form-item label="楼层" prop="floor">
                  <el-input-number v-model="form.floor" :min="1" :max="200" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="总楼层">
                  <el-input-number v-model="form.totalFloor" :min="1" :max="200" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="装修情况" prop="decoration">
                  <el-select v-model="form.decoration" style="width:100%">
                    <el-option label="精装" value="FINE" />
                    <el-option label="简装" value="SIMPLE" />
                    <el-option label="毛坯" value="ROUGH" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="可入住日期">
              <el-date-picker v-model="form.availableDate" type="date" placeholder="选择日期" style="width:100%" />
            </el-form-item>
          </el-card>

          <!-- Owner Type -->
          <el-card class="form-card">
            <template #header>房东类型</template>
            <el-form-item label="房东身份" prop="ownerType">
              <el-radio-group v-model="form.ownerType" class="owner-type-group">
                <el-radio value="OWNER" border>
                  <div class="radio-content">
                    <strong>一手房东</strong>
                    <span>直接出租自有房产，无中间商</span>
                  </div>
                </el-radio>
                <el-radio value="SUBLEASE" border>
                  <div class="radio-content">
                    <strong>二手房东</strong>
                    <span>转租房产，需支付给上级房东</span>
                  </div>
                </el-radio>
                <el-radio value="AGENT" border>
                  <div class="radio-content">
                    <strong>持牌中介</strong>
                    <span>持有中介执照，提供专业服务</span>
                  </div>
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-card>

          <!-- Price Info -->
          <el-card class="form-card">
            <template #header>租金信息</template>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="月租金(元)" prop="price">
                  <el-input-number v-model="form.price" :min="1" :max="999999" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="押金(月数)">
                  <el-input-number v-model="form.deposit" :min="0" :max="12" style="width:100%" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-card>

          <!-- Fee Config -->
          <el-card class="form-card">
            <template #header>五项费用配置</template>
            <div
              v-for="(fee, key) in feeLabels"
              :key="key"
              class="fee-config-row"
            >
              <span class="fee-name">{{ fee }}</span>
              <el-select v-model="form.feeConfig[key].type" style="width:160px">
                <el-option label="计量收费" value="METERED" />
                <el-option label="固定月费" value="FIXED" />
                <el-option label="房租已含" value="INCLUDED" />
              </el-select>
              <el-input-number
                v-if="form.feeConfig[key].type !== 'INCLUDED'"
                v-model="form.feeConfig[key].amount"
                :min="0"
                :precision="2"
                placeholder="金额"
                style="width:160px"
              />
              <span v-else class="included-text">已包含在房租中</span>
            </div>
          </el-card>

          <!-- Amenities -->
          <el-card class="form-card">
            <template #header>配套设施</template>
            <el-checkbox-group v-model="form.amenities" class="amenities-group">
              <el-checkbox
                v-for="item in amenityOptions"
                :key="item"
                :value="item"
              >{{ item }}</el-checkbox>
            </el-checkbox-group>
          </el-card>

          <!-- Images -->
          <el-card class="form-card">
            <template #header>房源图片</template>
            <div
              v-for="(img, i) in form.images"
              :key="i"
              class="image-row"
            >
              <el-input v-model="form.images[i]" placeholder="输入图片URL" />
              <el-button type="danger" circle @click="form.images.splice(i, 1)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button type="dashed" @click="form.images.push('')" class="add-image-btn">
              <el-icon><Plus /></el-icon> 添加图片URL
            </el-button>
          </el-card>

          <!-- Submit -->
          <div class="form-footer">
            <el-button @click="$router.back()">取消</el-button>
            <el-button type="primary" :loading="submitting" @click="handleSubmit" size="large">
              {{ isEdit ? '保存修改' : '发布房源' }}
            </el-button>
          </div>
        </el-form>
      </div>
    </div>
    <Footer />
  </div>
</template>

<script setup>
// 说明：发布/编辑房源页逻辑，根据路由是否携带 id 参数判断是发布新房源还是编辑已有房源
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import NavBar from '../components/NavBar.vue'
import Footer from '../components/Footer.vue'
import { createHouse, updateHouse, getHouseDetail } from '../api/house.js'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)      // 表单实例引用
const submitting = ref(false)  // 提交按钮 loading 状态

/** 通过路由参数判断当前是编辑模式（有 id）还是发布新房源模式 */
const isEdit = computed(() => !!route.params.id)

/** 五项费用字段的中文标签映射 */
const feeLabels = {
  waterFee: '水费',
  electricFee: '电费',
  gasFee: '燃气费',
  propertyFee: '物业费',
  internetFee: '网络费'
}

/** 配套设施选项列表（通过复选框多选） */
const amenityOptions = ['洗衣机', '空调', '冰箱', '热水器', 'WiFi', '停车位', '电梯', '床', '衣柜', '沙发', '电视', '微波炉', '天然气', '暖气']

// 房源表单数据（发布时为默认值，编辑时从接口加载）
const form = reactive({
  title: '',
  description: '',
  city: '',
  district: '',
  address: '',
  longitude: null,    // 可选：经度（用于地图定位）
  latitude: null,     // 可选：纬度
  area: 50,           // 面积（平米），默认50
  rooms: 2,           // 室（整数）
  halls: 1,           // 厅（整数）
  bathrooms: 1,       // 卫（整数）
  floor: 1,
  totalFloor: 20,
  decoration: 'SIMPLE',        // 装修情况：FINE/SIMPLE/ROUGH
  houseType: 'APARTMENT',      // 房屋类型：APARTMENT/HOUSE/ROOM/VILLA
  ownerType: 'OWNER',          // 房东类型：OWNER/SUBLEASE/AGENT
  price: 3000,                 // 月租金（元）
  deposit: 1,                  // 押金月数
  availableDate: null,         // 可入住日期（可选）
  // 五项费用配置：每项包含类型（METERED/FIXED/INCLUDED）和金额（UI 用）
  feeConfig: {
    waterFee: { type: 'METERED', amount: 3.5 },
    electricFee: { type: 'METERED', amount: 0.6 },
    gasFee: { type: 'METERED', amount: 2.5 },
    propertyFee: { type: 'FIXED', amount: 200 },
    internetFee: { type: 'FIXED', amount: 100 }
  },
  amenities: [],  // 配套设施列表（字符串数组，提交时转为 tags）
  images: []      // 图片 URL 列表（提交时转为 JSON 字符串）
})

// 表单必填项校验规则
const rules = {
  title: [{ required: true, message: '请输入房源标题', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  district: [{ required: true, message: '请输入区域', trigger: 'blur' }],
  address: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
  area: [{ required: true, message: '请输入面积', trigger: 'blur' }],
  rooms: [{ required: true, type: 'number', message: '请输入室数', trigger: 'blur' }],
  floor: [{ required: true, message: '请输入楼层', trigger: 'blur' }],
  decoration: [{ required: true, message: '请选择装修情况', trigger: 'change' }],
  ownerType: [{ required: true, message: '请选择房东类型', trigger: 'change' }],
  price: [{ required: true, message: '请输入月租金', trigger: 'blur' }]
}

onMounted(async () => {
  // 编辑模式：从接口加载已有房源数据填充表单
  if (isEdit.value) {
    try {
      const res = await getHouseDetail(route.params.id)
      // 映射后端扁平字段到前端表单结构
      form.title = res.title || ''
      form.description = res.description || ''
      form.city = res.city || ''
      form.district = res.district || ''
      form.address = res.address || ''
      form.longitude = res.longitude
      form.latitude = res.latitude
      form.area = res.area || 50
      form.rooms = res.rooms || 2
      form.halls = res.halls || 1
      form.bathrooms = res.bathrooms || 1
      form.floor = res.floor || 1
      form.totalFloor = res.totalFloor || 20
      form.decoration = res.decoration || 'SIMPLE'
      form.houseType = res.houseType || 'APARTMENT'
      form.ownerType = res.ownerType || 'OWNER'
      form.price = res.price || 3000
      form.deposit = res.deposit || 1
      form.availableDate = res.availableDate || null
      // 将后端扁平的费用字段还原为嵌套的 feeConfig
      form.feeConfig = {
        waterFee: { type: res.waterFeeType || 'METERED', amount: res.waterFee || 0 },
        electricFee: { type: res.electricFeeType || 'METERED', amount: res.electricFee || 0 },
        gasFee: { type: res.gasFeeType || 'METERED', amount: res.gasFee || 0 },
        propertyFee: { type: res.propertyFeeType || 'FIXED', amount: res.propertyFee || 0 },
        internetFee: { type: res.internetFeeType || 'FIXED', amount: res.internetFee || 0 }
      }
      // 将 tags 字符串还原为设施数组
      form.amenities = res.tags ? res.tags.split(',') : []
      // 将 images JSON 字符串还原为数组
      try {
        form.images = res.images ? JSON.parse(res.images) : []
      } catch {
        form.images = []
      }
    } catch (e) {
      ElMessage.error('加载房源信息失败')
    }
  }
})

/**
 * 提交房源表单
 * 将前端表单数据转换为后端 House 实体结构后提交
 * 编辑模式调用 updateHouse，发布模式调用 createHouse，成功后跳转到房东中心
 */
async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    // 获取费用金额：INCLUDED 类型费用为 0，其余取实际金额
    const feeAmount = (cfg) => cfg.type !== 'INCLUDED' ? cfg.amount : 0
    const validImages = form.images.filter(url => url)
    // 构造后端期望的扁平数据结构
    const payload = {
      title: form.title,
      description: form.description,
      city: form.city,
      district: form.district,
      address: form.address,
      longitude: form.longitude,
      latitude: form.latitude,
      area: form.area,
      rooms: form.rooms,
      halls: form.halls,
      bathrooms: form.bathrooms,
      floor: form.floor,
      totalFloor: form.totalFloor,
      decoration: form.decoration,
      houseType: form.houseType,
      ownerType: form.ownerType,
      price: form.price,
      deposit: form.deposit,
      availableDate: form.availableDate,
      // 将嵌套的 feeConfig 展开为扁平的费用字段
      waterFee: feeAmount(form.feeConfig.waterFee),
      waterFeeType: form.feeConfig.waterFee.type,
      electricFee: feeAmount(form.feeConfig.electricFee),
      electricFeeType: form.feeConfig.electricFee.type,
      gasFee: feeAmount(form.feeConfig.gasFee),
      gasFeeType: form.feeConfig.gasFee.type,
      propertyFee: feeAmount(form.feeConfig.propertyFee),
      propertyFeeType: form.feeConfig.propertyFee.type,
      internetFee: feeAmount(form.feeConfig.internetFee),
      internetFeeType: form.feeConfig.internetFee.type,
      // 将设施数组转为逗号分隔的标签字符串
      tags: form.amenities.length > 0 ? form.amenities.join(',') : null,
      // 将图片数组转为 JSON 字符串
      images: validImages.length > 0 ? JSON.stringify(validImages) : null
    }
    if (isEdit.value) {
      await updateHouse(route.params.id, payload)
      ElMessage.success('房源更新成功')
    } else {
      await createHouse(payload)
      ElMessage.success('房源发布成功，已直接上线')
    }
    router.push('/landlord-center')  // 返回房东中心查看房源状态
  } catch (e) {
    ElMessage.error(e.message || '操作失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.publish-house-page {
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
  max-width: 900px;
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

.publish-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-card {
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}

.owner-type-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.owner-type-group :deep(.el-radio.is-bordered) {
  height: auto;
  padding: 12px 16px;
}

.radio-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-left: 4px;
}

.radio-content strong {
  font-size: 14px;
  color: #303133;
}

.radio-content span {
  font-size: 12px;
  color: #909399;
}

.fee-config-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 6px;
}

.fee-name {
  width: 60px;
  font-weight: 500;
  color: #303133;
  flex-shrink: 0;
}

.included-text {
  color: #67c23a;
  font-size: 13px;
}

.amenities-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.image-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.add-image-btn {
  width: 100%;
}

.form-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 0;
}

.form-footer :deep(.el-button--primary) {
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
  border-radius: 8px;
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.form-footer :deep(.el-button--primary:hover) {
  opacity: 0.9;
  transform: translateY(-1px);
}
</style>
