<template>
  <el-card class="house-card" shadow="hover" @click="goDetail">
    <div class="card-image">
      <img
        :src="house.images && house.images.length > 0 ? house.images[0] : placeholder"
        :alt="house.title"
        class="house-img"
      />
      <div class="badge-overlay">
        <OwnerTypeBadge :ownerType="house.ownerType" />
      </div>
    </div>
    <div class="card-body">
      <h3 class="title">{{ house.title }}</h3>
      <p class="address">
        <el-icon><Location /></el-icon>
        {{ house.city }} {{ house.district }} {{ house.address }}
      </p>
      <div class="meta">
        <span class="rooms">{{ house.rooms }}</span>
        <span class="area">{{ house.area }}㎡</span>
        <el-tag size="small" type="info">{{ decorationLabel }}</el-tag>
      </div>
      <div class="price-row">
        <span class="price">¥{{ house.price }}</span>
        <span class="unit">元/月</span>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import OwnerTypeBadge from './OwnerTypeBadge.vue'

const props = defineProps({
  house: {
    type: Object,
    required: true
  }
})

const router = useRouter()
const placeholder = 'https://via.placeholder.com/400x300/409EFF/ffffff?text=房屋图片'

const decorationLabel = computed(() => {
  const map = { FINE: '精装', SIMPLE: '简装', ROUGH: '毛坯' }
  return map[props.house.decoration] || props.house.decoration || '未知'
})

function goDetail() {
  router.push(`/houses/${props.house.id}`)
}
</script>

<style scoped>
.house-card {
  cursor: pointer;
  transition: transform 0.2s;
  border-radius: 8px;
  overflow: hidden;
}

.house-card:hover {
  transform: translateY(-4px);
}

.card-image {
  position: relative;
  height: 200px;
  overflow: hidden;
  margin: -20px -20px 0 -20px;
}

.house-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.badge-overlay {
  position: absolute;
  top: 8px;
  right: 8px;
}

.card-body {
  padding-top: 12px;
}

.title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 6px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.address {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 13px;
  color: #606266;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.price {
  font-size: 22px;
  font-weight: 700;
  color: #f56c6c;
}

.unit {
  font-size: 13px;
  color: #909399;
}
</style>
