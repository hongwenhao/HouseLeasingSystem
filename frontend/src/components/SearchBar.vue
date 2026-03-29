<template>
  <!-- 组件说明：房源搜索栏组件，提供关键词、城市、区域、价格区间、
       房间数、房东类型、装修情况等多维度筛选条件输入。
       通过 v-model（modelValue）与父组件双向绑定筛选对象，
       点击搜索按钮触发 search 事件通知父组件发起请求。 -->
  <div class="search-bar">
    <el-row :gutter="12">
      <!-- 关键词搜索框 -->
      <el-col :xs="24" :sm="12" :md="6">
        <el-input
          v-model="localFilters.keyword"
          placeholder="搜索关键词..."
          clearable
          prefix-icon="Search"
        />
      </el-col>
      <!-- 省份筛选 -->
      <el-col :xs="12" :sm="6" :md="3">
        <el-select
          v-model="localFilters.province"
          placeholder="省份"
          clearable
          filterable
          style="width:100%"
        >
          <el-option
            v-for="item in provinceOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-col>
      <!-- 城市筛选（随省份联动） -->
      <el-col :xs="12" :sm="6" :md="3">
        <el-select
          v-model="localFilters.city"
          placeholder="城市"
          clearable
          filterable
          :disabled="cityOptions.length === 0"
          style="width:100%"
        >
          <el-option
            v-for="item in cityOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-col>
      <!-- 区域筛选（随城市联动） -->
      <el-col :xs="12" :sm="6" :md="3">
        <el-select
          v-model="localFilters.district"
          placeholder="区域"
          clearable
          filterable
          :disabled="!localFilters.city"
          style="width:100%"
        >
          <el-option
            v-for="item in districtOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-col>
      <!-- 最低租金输入 -->
      <el-col :xs="12" :sm="6" :md="3">
        <el-input
          v-model.number="localFilters.minPrice"
          placeholder="最低租金"
          type="number"
          clearable
        />
      </el-col>
      <!-- 最高租金输入 -->
      <el-col :xs="12" :sm="6" :md="3">
        <el-input
          v-model.number="localFilters.maxPrice"
          placeholder="最高租金"
          type="number"
          clearable
        />
      </el-col>
      <!-- 房间数下拉选择 -->
      <el-col :xs="12" :sm="6" :md="3">
        <el-select v-model="localFilters.rooms" placeholder="房间数" clearable style="width:100%">
          <el-option label="不限" :value="null" />
          <el-option label="1室" :value="1" />
          <el-option label="2室" :value="2" />
          <el-option label="3室" :value="3" />
          <el-option label="4+室" :value="4" />
        </el-select>
      </el-col>
      <!-- 房东类型下拉选择 -->
      <el-col :xs="12" :sm="6" :md="4">
        <el-select v-model="localFilters.ownerType" placeholder="房东类型" clearable style="width:100%">
          <el-option label="不限" value="" />
          <el-option label="一手房东" value="OWNER" />
          <el-option label="二手房东" value="SUBLEASE" />
          <el-option label="中介" value="AGENT" />
        </el-select>
      </el-col>
      <!-- 装修情况下拉选择 -->
      <el-col :xs="12" :sm="6" :md="4">
        <el-select v-model="localFilters.decoration" placeholder="装修情况" clearable style="width:100%">
          <el-option label="不限" value="" />
          <el-option label="精装" value="FINE" />
          <el-option label="简装" value="SIMPLE" />
          <el-option label="毛坯" value="ROUGH" />
        </el-select>
      </el-col>
      <!-- 搜索按钮 -->
      <el-col :xs="24" :sm="6" :md="4">
        <el-button type="primary" @click="handleSearch" style="width:100%">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
// 说明：搜索栏组件，管理本地筛选状态并通过事件与父组件通信
import { computed, reactive, watch } from 'vue'
import { regionData } from 'element-china-area-data'

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({})  // 父组件传入的当前筛选值
  }
})

// 声明组件向外发送的事件：update:modelValue（v-model 更新）和 search（触发搜索）
const emit = defineEmits(['update:modelValue', 'search'])

// 本地筛选状态对象，初始化时合并父组件传入的值
const localFilters = reactive({
  keyword: '',
  province: '',
  city: '',
  district: '',
  minPrice: null,
  maxPrice: null,
  rooms: '',
  ownerType: '',
  decoration: '',
  ...props.modelValue  // 支持父组件预设初始筛选值（如从 URL query 参数初始化）
})

// 行政区划数据中的分组节点标签（不是真实的城市名，而是数据中的分组占位符）
const GROUPING_NODE_LABELS = ['市辖区', '县', '省直辖县级行政区划']
const EXCLUDED_REGIONS = ['香港特别行政区', '澳门特别行政区', '台湾省']

// 构建三级联动数据：省份 → 城市 → 区域
// 对于直辖市（北京、天津、上海、重庆），其下级只有"市辖区"等分组节点，
// 需要将分组节点的子项（真实区县）直接作为区域，城市则使用直辖市名称本身。
const allProvinceData = regionData
  .filter((p) => !EXCLUDED_REGIONS.includes(p.label))
  .map((province) => {
    const realCities = (province.children || []).filter(
      (c) => !GROUPING_NODE_LABELS.includes(c.label)
    )
    const groupingNodes = (province.children || []).filter(
      (c) => GROUPING_NODE_LABELS.includes(c.label)
    )

    let cities
    if (realCities.length > 0) {
      // 普通省份：直接使用真实城市及其区县
      cities = realCities.map((city) => ({
        label: city.label,
        value: city.label,
        districts: (city.children || []).map((d) => ({ label: d.label, value: d.label }))
      }))
    } else {
      // 直辖市：将所有分组节点下的区县合并，城市名称使用直辖市本身
      const districts = groupingNodes
        .flatMap((g) => g.children || [])
        .map((d) => ({ label: d.label, value: d.label }))
      cities = [{ label: province.label, value: province.label, districts }]
    }

    return { label: province.label, value: province.label, cities }
  })

// 省份下拉选项
const provinceOptions = allProvinceData.map((p) => ({ label: p.label, value: p.value }))

// 城市下拉选项（依赖选中的省份）
const cityOptions = computed(() => {
  const province = allProvinceData.find((p) => p.value === localFilters.province)
  return province?.cities || []
})

// 区域下拉选项（依赖选中的城市）
const districtOptions = computed(() => {
  const province = allProvinceData.find((p) => p.value === localFilters.province)
  if (!province) return []
  const city = province.cities.find((c) => c.value === localFilters.city)
  return city?.districts || []
})

// 监听本地筛选状态变化，实时同步到父组件（实现 v-model 双向绑定）
watch(localFilters, (val) => {
  emit('update:modelValue', { ...val })
})

// 省份变化时，自动清空下级城市（区域会由城市 watcher 连带清空）
watch(() => localFilters.province, () => {
  localFilters.city = ''
})

// 城市变化时，自动清空下级区域选择
watch(() => localFilters.city, () => {
  localFilters.district = ''
})

/** 点击搜索按钮：同步筛选值到父组件并触发 search 事件 */
function handleSearch() {
  emit('update:modelValue', { ...localFilters })
  emit('search', { ...localFilters })
}
</script>

<style scoped>
/* ===== 搜索栏容器：圆角 + 柔和阴影 ===== */
.search-bar {
  padding: 20px;
  background: #fff;
  border-radius: 12px;
}

/* 行间距：每行筛选器之间保持 12px 垂直间距 */
.el-row {
  row-gap: 12px;
}
</style>
