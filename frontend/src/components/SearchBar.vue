<template>
  <div class="search-bar">
    <el-row :gutter="12">
      <el-col :xs="24" :sm="12" :md="6">
        <el-input
          v-model="localFilters.keyword"
          placeholder="搜索关键词..."
          clearable
          prefix-icon="Search"
        />
      </el-col>
      <el-col :xs="12" :sm="6" :md="3">
        <el-input v-model="localFilters.city" placeholder="城市" clearable />
      </el-col>
      <el-col :xs="12" :sm="6" :md="3">
        <el-input v-model="localFilters.district" placeholder="区域" clearable />
      </el-col>
      <el-col :xs="12" :sm="6" :md="3">
        <el-input
          v-model.number="localFilters.minPrice"
          placeholder="最低租金"
          type="number"
          clearable
        />
      </el-col>
      <el-col :xs="12" :sm="6" :md="3">
        <el-input
          v-model.number="localFilters.maxPrice"
          placeholder="最高租金"
          type="number"
          clearable
        />
      </el-col>
      <el-col :xs="12" :sm="6" :md="3">
        <el-select v-model="localFilters.rooms" placeholder="房间数" clearable style="width:100%">
          <el-option label="不限" :value="null" />
          <el-option label="1室" :value="1" />
          <el-option label="2室" :value="2" />
          <el-option label="3室" :value="3" />
          <el-option label="4+室" :value="4" />
        </el-select>
      </el-col>
      <el-col :xs="12" :sm="6" :md="4">
        <el-select v-model="localFilters.ownerType" placeholder="房东类型" clearable style="width:100%">
          <el-option label="不限" value="" />
          <el-option label="一手房东" value="OWNER" />
          <el-option label="二手房东" value="SUBLEASE" />
          <el-option label="中介" value="AGENT" />
        </el-select>
      </el-col>
      <el-col :xs="12" :sm="6" :md="4">
        <el-select v-model="localFilters.decoration" placeholder="装修情况" clearable style="width:100%">
          <el-option label="不限" value="" />
          <el-option label="精装" value="FINE" />
          <el-option label="简装" value="SIMPLE" />
          <el-option label="毛坯" value="ROUGH" />
        </el-select>
      </el-col>
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
import { reactive, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:modelValue', 'search'])

const localFilters = reactive({
  keyword: '',
  city: '',
  district: '',
  minPrice: null,
  maxPrice: null,
  rooms: '',
  ownerType: '',
  decoration: '',
  ...props.modelValue
})

watch(localFilters, (val) => {
  emit('update:modelValue', { ...val })
})

function handleSearch() {
  emit('update:modelValue', { ...localFilters })
  emit('search', { ...localFilters })
}
</script>

<style scoped>
.search-bar {
  padding: 16px;
  background: #fff;
  border-radius: 8px;
}

.el-row {
  row-gap: 12px;
}
</style>
