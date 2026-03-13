<template>
  <!-- 组件说明：房源卡片组件，以缩略图+关键信息的形式展示单套房源。
       点击卡片整体跳转到对应的房源详情页。
       包含：房源封面图、房东类型角标、标题、地址、户型面积、装修情况、月租价格。 -->
  <el-card class="house-card" shadow="hover" @click="goDetail">
    <!-- 房源封面图区域 -->
    <div class="card-image">
      <img
        :src="house.images && house.images.length > 0 ? house.images[0] : placeholder"
        :alt="house.title"
        class="house-img"
      />
      <!-- 右上角房东类型角标（一手房东 / 二手房东 / 中介） -->
      <div class="badge-overlay">
        <OwnerTypeBadge :ownerType="house.ownerType" />
      </div>
    </div>
    <!-- 房源信息主体 -->
    <div class="card-body">
      <h3 class="title">{{ house.title }}</h3>
      <!-- 地址：城市 + 区域 + 详细地址 -->
      <p class="address">
        <el-icon><Location /></el-icon>
        {{ house.city }} {{ house.district }} {{ house.address }}
      </p>
      <!-- 房源元信息：室数 / 面积 / 装修情况 -->
      <div class="meta">
        <span class="rooms">{{ house.rooms }}</span>
        <span class="area">{{ house.area }}㎡</span>
        <el-tag size="small" type="info">{{ decorationLabel }}</el-tag>
      </div>
      <!-- 月租金显示（红色强调） -->
      <div class="price-row">
        <span class="price">¥{{ house.price }}</span>
        <span class="unit">元/月</span>
      </div>
    </div>
  </el-card>
</template>

<script setup>
// 说明：房源卡片组件，展示单套房源的摘要信息，点击跳转详情页
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import OwnerTypeBadge from './OwnerTypeBadge.vue'

const props = defineProps({
  house: {
    type: Object,
    required: true  // 父组件必须传入完整的房源数据对象
  }
})

const router = useRouter()
// 图片加载失败时显示的占位图 URL
const placeholder = 'https://via.placeholder.com/400x300/409EFF/ffffff?text=房屋图片'

/**
 * 将装修枚举值映射为中文标签
 * FINE → 精装，SIMPLE → 简装，ROUGH → 毛坯
 */
const decorationLabel = computed(() => {
  const map = { FINE: '精装', SIMPLE: '简装', ROUGH: '毛坯' }
  return map[props.house.decoration] || props.house.decoration || '未知'
})

/** 跳转到该房源的详情页 */
function goDetail() {
  router.push(`/houses/${props.house.id}`)
}
</script>

<style scoped>
/* ===== 卡片容器 ===== */
.house-card {
  cursor: pointer;
  transition: transform 0.2s;  /* 悬停时上浮动效 */
  border-radius: 8px;
  overflow: hidden;
}

.house-card:hover {
  transform: translateY(-4px);
}

/* ===== 图片区域 ===== */
.card-image {
  position: relative;
  height: 200px;
  overflow: hidden;
  /* 负边距使图片覆盖 el-card 的默认内边距 */
  margin: -20px -20px 0 -20px;
}

.house-img {
  width: 100%;
  height: 100%;
  object-fit: cover;  /* 图片裁剪填充，保持宽高比 */
}

/* 房东类型角标：覆盖在图片右上角 */
.badge-overlay {
  position: absolute;
  top: 8px;
  right: 8px;
}

/* ===== 信息主体 ===== */
.card-body {
  padding-top: 12px;
}

.title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 6px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;  /* 标题过长时截断显示省略号 */
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

/* ===== 价格区域 ===== */
.price-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.price {
  font-size: 22px;
  font-weight: 700;
  color: #f56c6c;  /* 红色强调租金价格 */
}

.unit {
  font-size: 13px;
  color: #909399;
}
</style>

