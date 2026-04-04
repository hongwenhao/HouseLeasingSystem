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
            <el-form-item label="省份" prop="province">
              <el-select v-model="form.province" placeholder="请选择省份" clearable filterable style="width:100%" @change="onProvinceChange">
                <el-option
                  v-for="item in provinceOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="城市" prop="city">
                  <el-select v-model="form.city" :placeholder="cityOptionsForForm.length === 0 ? '请先选择省份' : '请选择城市'" clearable filterable style="width:100%" :disabled="cityOptionsForForm.length === 0" @change="onCityChange">
                    <el-option
                      v-for="item in cityOptionsForForm"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="区域" prop="district">
                  <el-select v-model="form.district" :placeholder="!form.city ? '请先选择城市' : '请选择区域'" clearable filterable style="width:100%" :disabled="!form.city">
                    <el-option
                      v-for="item in districtOptionsForForm"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="详细地址" prop="address">
              <el-input v-model="form.address" placeholder="请输入详细地址" />
            </el-form-item>
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
                  <!--
                    户型输入改为可换行布局：
                    1) 避免在中等分辨率下第三组“卫”被遮挡；
                    2) 通过统一的 item 容器保持“室/厅/卫”对齐一致。
                  -->
                  <div class="room-layout-row">
                    <div class="room-layout-item">
                      <el-input-number v-model="form.rooms" :min="1" :max="20" controls-position="right" class="room-layout-input" />
                      <span>室</span>
                    </div>
                    <div class="room-layout-item">
                      <el-input-number v-model="form.halls" :min="0" :max="10" controls-position="right" class="room-layout-input" />
                      <span>厅</span>
                    </div>
                    <div class="room-layout-item">
                      <el-input-number v-model="form.bathrooms" :min="0" :max="10" controls-position="right" class="room-layout-input" />
                      <span>卫</span>
                    </div>
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
                    <el-option label="中等装修" value="MEDIUM" />
                    <el-option label="毛坯" value="ROUGH" />
                    <el-option label="豪装" value="LUXURY" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <!-- 可入住日期改为必填：补充 prop 后接入 rules 校验 -->
            <el-form-item label="可入住日期" prop="availableDate">
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
            <!-- 图片上传：选择文件后自动调用 handleLocalImageChange 上传至服务端，表单中只保存图片 URL -->
            <el-upload
              action="#"
              :auto-upload="false"
              :show-file-list="false"
              accept="image/*"
              :on-change="handleLocalImageChange"
            >
              <el-button type="primary" plain>本地上传图片</el-button>
            </el-upload>
            <div
              v-for="(img, i) in form.images"
              :key="i"
              class="uploaded-image-row"
            >
              <img :src="img" :alt="`房源图片${i + 1}`" class="uploaded-image-thumb" />
              <el-button type="danger" text @click="form.images.splice(i, 1)">删除</el-button>
            </div>
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
import { regionData } from 'element-china-area-data'
import NavBar from '../components/NavBar.vue'
import Footer from '../components/Footer.vue'
import { createHouse, updateHouse, getHouseDetail, uploadHouseImage } from '../api/house.js'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)      // 表单实例引用
const submitting = ref(false)  // 提交按钮 loading 状态

/** 通过路由参数判断当前是编辑模式（有 id）还是发布新房源模式 */
const isEdit = computed(() => !!route.params.id)

// 行政区划数据中的分组节点标签
const GROUPING_NODE_LABELS = ['市辖区', '县', '省直辖县级行政区划']
const EXCLUDED_REGIONS = ['香港特别行政区', '澳门特别行政区', '台湾省']

/**
 * 将“区域候选节点”转为真正可选的区县列表。
 * 说明：
 * - 行政区划库中经常存在“市辖区/县/省直辖县级行政区划”等分组占位节点；
 * - 这些分组节点不应直接展示给用户，需要展开其 children 作为最终可选区域；
 * - 普通区县节点则直接保留。
 */
function normalizeDistricts(districtCandidates = []) {
  return districtCandidates.flatMap((districtNode) => {
    // 防御式处理：容错空节点，避免读取 label 时触发运行时异常
    if (!districtNode || !districtNode.label) return []
    if (GROUPING_NODE_LABELS.includes(districtNode.label)) {
      return (districtNode.children || [])
        .filter((child) => child && child.label)
        .map((child) => ({ label: child.label, value: child.label }))
    }
    return [{ label: districtNode.label, value: districtNode.label }]
  })
}

// 构建省市区三级联动数据（过滤分组占位节点，避免出现“市辖区”等选项）
const allProvinceData = regionData
  .filter((p) => !EXCLUDED_REGIONS.includes(p.label))
  .map((province) => {
    const realCities = (province.children || []).filter((c) => !GROUPING_NODE_LABELS.includes(c.label))
    const groupingNodes = (province.children || []).filter((c) => GROUPING_NODE_LABELS.includes(c.label))
    let cities
    if (realCities.length > 0) {
      cities = realCities.map((city) => ({
        label: city.label,
        value: city.label,
        districts: normalizeDistricts(city.children || [])
      }))
    } else {
      const districts = normalizeDistricts(groupingNodes)
      cities = [{ label: province.label, value: province.label, districts }]
    }
    return { label: province.label, value: province.label, cities }
  })

// 省份下拉选项
const provinceOptions = allProvinceData.map((p) => ({ label: p.label, value: p.value }))

// 城市下拉选项（依赖选中的省份）
const cityOptionsForForm = computed(() => {
  const province = allProvinceData.find((p) => p.value === form.province)
  return province?.cities || []
})

// 区域下拉选项（依赖选中的城市）
const districtOptionsForForm = computed(() => {
  const province = allProvinceData.find((p) => p.value === form.province)
  if (!province) return []
  const city = province.cities.find((c) => c.value === form.city)
  return city?.districts || []
})

// 省份变化时清空下级城市和区域
function onProvinceChange() {
  form.city = ''
  form.district = ''
}

// 城市变化时清空下级区域
function onCityChange() {
  form.district = ''
}

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
  province: '',
  city: '',
  district: '',
  address: '',
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
  province: [{ required: true, message: '请选择省份', trigger: 'change' }],
  city: [{ required: true, message: '请选择城市', trigger: 'change' }],
  district: [{ required: true, message: '请选择区域', trigger: 'change' }],
  address: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
  area: [{ required: true, message: '请输入面积', trigger: 'blur' }],
  rooms: [{ required: true, type: 'number', message: '请输入室数', trigger: 'blur' }],
  floor: [{ required: true, message: '请输入楼层', trigger: 'blur' }],
  decoration: [{ required: true, message: '请选择装修情况', trigger: 'change' }],
  ownerType: [{ required: true, message: '请选择房东类型', trigger: 'change' }],
  availableDate: [{ required: true, message: '请选择可入住日期', trigger: 'change' }],
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
      form.province = res.province || ''
      form.city = res.city || ''
      form.district = res.district || ''
      form.address = res.address || ''
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
      province: form.province,
      city: form.city,
      district: form.district,
      address: form.address,
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
      ElMessage.success('房源发布成功，已上线')
    }
    router.push('/landlord-center')  // 返回房东中心查看房源状态
  } catch (e) {
    ElMessage.error(e.message || '操作失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

/**
 * 上传图片到服务器并将返回的图片 URL 存入表单列表
 *
 * 说明：不再将图片转为 base64 存入数据库（体积过大），
 * 改为通过 POST /api/upload/image 将文件上传至服务端磁盘，
 * 后端返回可供 HTTP 访问的图片路径（/api/uploads/xxx.jpg），
 * 表单及数据库中只存储该 URL 字符串。
 *
 * @param {object} uploadFile - el-upload 组件传入的文件对象（含 raw 属性）
 */
async function handleLocalImageChange(uploadFile) {
  const rawFile = uploadFile?.raw
  if (!rawFile) return
  if (!rawFile.type?.startsWith('image/')) {
    ElMessage.warning('仅支持上传图片文件')
    return
  }
  try {
    // 将文件上传到服务端，接口返回图片访问 URL
    const imageUrl = await uploadHouseImage(rawFile)
    form.images.push(imageUrl)
    ElMessage.success('图片上传成功')
  } catch (e) {
    ElMessage.error(e?.message || '图片上传失败，请重试')
  }
}
</script>

<style scoped>
.publish-house-page {
  --room-layout-input-width: 88px;
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

.owner-type-group :deep(.el-radio) {
  align-items: flex-start;
}

.owner-type-group :deep(.el-radio__label) {
  width: 100%;
  white-space: normal;
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

/* 户型输入：统一项布局并允许换行，防止“卫”字在窄宽度被遮挡 */
.room-layout-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.room-layout-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.room-layout-input {
  /* 88px 可完整容纳 el-input-number（含控制按钮） 与单位文字，避免“卫”被压缩遮挡 */
  width: var(--room-layout-input-width);
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

.uploaded-image-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.uploaded-image-thumb {
  width: 120px;
  height: 80px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #ebeef5;
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
