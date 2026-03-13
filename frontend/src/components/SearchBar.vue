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
      <!-- 城市筛选 -->
      <el-col :xs="12" :sm="6" :md="3">
        <el-input v-model="localFilters.city" placeholder="城市" clearable />
      </el-col>
      <!-- 区域筛选 -->
      <el-col :xs="12" :sm="6" :md="3">
        <el-input v-model="localFilters.district" placeholder="区域" clearable />
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
import { reactive, watch } from 'vue'

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
  city: '',
  district: '',
  minPrice: null,
  maxPrice: null,
  rooms: '',
  ownerType: '',
  decoration: '',
  ...props.modelValue  // 支持父组件预设初始筛选值（如从 URL query 参数初始化）
})

// 监听本地筛选状态变化，实时同步到父组件（实现 v-model 双向绑定）
watch(localFilters, (val) => {
  emit('update:modelValue', { ...val })
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

