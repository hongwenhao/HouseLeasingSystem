<template>
  <!-- 组件说明：房源搜索栏组件，提供关键词、城市、区域、价格区间、
       房间数、房东类型、装修情况等多维度筛选条件输入。
       通过 v-model（modelValue）与父组件双向绑定筛选对象，
       点击搜索按钮触发 search 事件通知父组件发起请求。
       compact=true 时渲染为首页用的单行横向搜索栏；
       默认为垂直堆叠布局，适合放置在侧边栏中。 -->

  <!-- 紧凑横向模式（首页 Hero 区域使用） -->
  <div v-if="compact" class="compact-bar">
    <el-input
      v-model="localFilters.keyword"
      placeholder="搜索关键词、小区名..."
      clearable
      :prefix-icon="Search"
      class="compact-keyword"
    />
    <el-select
      v-model="localFilters.city"
      placeholder="选择城市"
      clearable
      filterable
      class="compact-city"
    >
      <el-option
        v-for="city in allCitiesFlat"
        :key="city.value"
        :label="city.label"
        :value="city.value"
      />
    </el-select>
    <el-select v-model="localFilters.rooms" placeholder="房间数" clearable class="compact-rooms">
      <el-option label="不限" :value="null" />
      <el-option label="1室" :value="1" />
      <el-option label="2室" :value="2" />
      <el-option label="3室" :value="3" />
      <el-option label="4+室" :value="4" />
    </el-select>
    <el-select v-model="compactPriceRange" placeholder="价格区间" clearable class="compact-price">
      <el-option label="1500元以下" value="0-1500" />
      <el-option label="1500~3000元" value="1500-3000" />
      <el-option label="3000~5000元" value="3000-5000" />
      <el-option label="5000~8000元" value="5000-8000" />
      <el-option label="8000元以上" value="8000-" />
    </el-select>
    <el-button type="primary" class="compact-btn" @click="handleSearch">
      <el-icon><Search /></el-icon>
      搜索
    </el-button>
  </div>

  <!-- 完整侧边栏模式（房源列表页使用） -->
  <div v-else class="search-bar">
    <div class="search-bar-header">
      <el-icon class="header-icon"><Filter /></el-icon>
      <span class="header-title">筛选条件</span>
    </div>

    <!-- 关键词搜索框 -->
    <div class="filter-group">
      <div class="filter-label">关键词</div>
      <el-input
        v-model="localFilters.keyword"
        placeholder="搜索关键词..."
        clearable
        :prefix-icon="Search"
      />
    </div>

    <!-- 省份筛选 -->
    <div class="filter-group">
      <div class="filter-label">省份</div>
      <el-select
        v-model="localFilters.province"
        placeholder="选择省份"
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
    </div>

    <!-- 城市筛选（随省份联动） -->
    <div class="filter-group">
      <div class="filter-label">城市</div>
      <el-select
        v-model="localFilters.city"
        placeholder="选择城市"
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
    </div>

    <!-- 区域筛选（随城市联动） -->
    <div class="filter-group">
      <div class="filter-label">区域</div>
      <el-select
        v-model="localFilters.district"
        placeholder="选择区域"
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
    </div>

    <!-- 租金区间 -->
    <div class="filter-group">
      <div class="filter-label">租金区间（元/月）</div>
      <div class="price-range">
        <el-input
          v-model.number="localFilters.minPrice"
          placeholder="最低"
          type="number"
          clearable
        />
        <span class="price-divider">~</span>
        <el-input
          v-model.number="localFilters.maxPrice"
          placeholder="最高"
          type="number"
          clearable
        />
      </div>
    </div>

    <!-- 房间数下拉选择 -->
    <div class="filter-group">
      <div class="filter-label">房间数</div>
      <el-select v-model="localFilters.rooms" placeholder="不限" clearable style="width:100%">
        <el-option label="不限" :value="null" />
        <el-option label="1室" :value="1" />
        <el-option label="2室" :value="2" />
        <el-option label="3室" :value="3" />
        <el-option label="4+室" :value="4" />
      </el-select>
    </div>

    <!-- 房东类型下拉选择 -->
    <div class="filter-group">
      <div class="filter-label">房东类型</div>
      <el-select v-model="localFilters.ownerType" placeholder="不限" clearable style="width:100%">
        <el-option label="不限" value="" />
        <el-option label="一手房东" value="OWNER" />
        <el-option label="二手房东" value="SUBLEASE" />
        <el-option label="中介" value="AGENT" />
      </el-select>
    </div>

    <!-- 装修情况下拉选择 -->
    <div class="filter-group">
      <div class="filter-label">装修情况</div>
      <el-select v-model="localFilters.decoration" placeholder="不限" clearable style="width:100%">
        <el-option label="不限" value="" />
        <el-option label="精装" value="FINE" />
        <el-option label="简装" value="SIMPLE" />
        <el-option label="中等装修" value="MEDIUM" />
        <el-option label="毛坯" value="ROUGH" />
        <el-option label="豪装" value="LUXURY" />
      </el-select>
    </div>

    <!-- 搜索按钮 -->
    <el-button type="primary" class="search-btn" @click="handleSearch">
      <el-icon><Search /></el-icon>
      搜索
    </el-button>
  </div>

</template>

<script setup>
// 说明：搜索栏组件，管理本地筛选状态并通过事件与父组件通信
import { computed, reactive, ref, watch } from 'vue'
import { regionData } from 'element-china-area-data'
import { Filter, Search } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({})  // 父组件传入的当前筛选值
  },
  compact: {
    type: Boolean,
    default: false  // true 时渲染为首页单行横向搜索栏
  }
})

// 声明组件向外发送的事件：update:modelValue（v-model 更新）和 search（触发搜索）
const emit = defineEmits(['update:modelValue', 'search'])

// 过滤占位的分组名称，防止“市辖区”等分组节点被当成真实城市显示
const sanitizeAreaValue = (val) => (val && GROUPING_NODE_LABELS.includes(val) ? '' : (val || ''))
const initialFilters = {
  ...props.modelValue,
  city: sanitizeAreaValue(props.modelValue.city),
  district: sanitizeAreaValue(props.modelValue.district)
}

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
  ...initialFilters  // 支持父组件预设初始筛选值（如从 URL query 参数初始化）
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

// 紧凑模式：所有城市扁平列表（不含省份/区县）
const allCitiesFlat = computed(() =>
  allProvinceData.flatMap((p) => p.cities.map((c) => ({ label: c.label, value: c.value })))
)

// 紧凑模式：价格区间快速选项
// 根据已有的 minPrice/maxPrice 初始化对应的快速区间选项
function detectCompactPriceRange(min, max) {
  if (min === 0 && max === 1500) return '0-1500'
  if (min === 1500 && max === 3000) return '1500-3000'
  if (min === 3000 && max === 5000) return '3000-5000'
  if (min === 5000 && max === 8000) return '5000-8000'
  if (min === 8000 && max == null) return '8000-'
  return ''
}
const compactPriceRange = ref(detectCompactPriceRange(localFilters.minPrice, localFilters.maxPrice))
watch(compactPriceRange, (val) => {
  if (!val) {
    localFilters.minPrice = null
    localFilters.maxPrice = null
  } else {
    const [min, max] = val.split('-')
    localFilters.minPrice = min ? Number(min) : null
    localFilters.maxPrice = max ? Number(max) : null
  }
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

// 如果外部值意外传入“市辖区/县”等分组节点，占位值会被清空，确保下拉列表不出现占位名称
function clearGroupingNode(field) {
  if (GROUPING_NODE_LABELS.includes(localFilters[field])) {
    localFilters[field] = ''
  }
}
watch(() => localFilters.city, () => clearGroupingNode('city'))
watch(() => localFilters.district, () => clearGroupingNode('district'))

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
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* 搜索栏标题头部 */
.search-bar-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f2f5;
}

.header-icon {
  font-size: 16px;
  color: #667eea;
}

.header-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
}

/* 每个筛选项分组：标签 + 输入控件 */
.filter-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

/* 筛选标签文字 */
.filter-label {
  font-size: 12px;
  color: #909399;
  font-weight: 500;
  line-height: 1;
}

/* 租金区间：两个输入框并排 */
.price-range {
  display: flex;
  align-items: center;
  gap: 6px;
}

.price-range .el-input {
  flex: 1;
  min-width: 0;
}

.price-divider {
  color: #c0c4cc;
  font-size: 13px;
  flex-shrink: 0;
}

/* 搜索按钮：全宽 + 渐变背景 */
.search-btn {
  width: 100%;
  margin-top: 4px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
}

.search-btn:hover {
  opacity: 0.9;
}

/* ===== 紧凑横向模式（首页 Hero 搜索栏） ===== */
.compact-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  flex-wrap: wrap;
}

.compact-keyword {
  flex: 2;
  min-width: 180px;
}

.compact-city,
.compact-rooms,
.compact-price {
  flex: 1;
  min-width: 130px;
}

/* 紧凑模式搜索按钮：渐变背景 + 固定高度 */
.compact-btn {
  flex-shrink: 0;
  height: 32px;
  padding: 0 22px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.compact-btn:hover {
  opacity: 0.9;
}
</style>
