<template>
  <!-- 组件说明：合同风险提示组件，展示 AI 智能检测到的合同风险条款列表。
       仅在存在风险项时渲染（v-if），依据最高风险等级展示综合风险级别。
       风险等级：HIGH（高风险，红色）/ MEDIUM（中等风险，橙色）/ LOW（低风险，蓝色） -->
  <div class="risk-warning" v-if="risks && risks.length > 0">
    <!-- 风险提示头部：显示风险总数和整体风险等级 -->
    <el-alert
      :title="`合同风险提示（共 ${risks.length} 项风险）`"
      :type="overallType"
      :closable="false"
      show-icon
      class="risk-header"
    />
    <!-- 风险列表：逐条展示风险描述和等级 -->
    <div class="risk-list">
      <div
        v-for="(risk, index) in risks"
        :key="index"
        class="risk-item"
      >
        <!-- 风险等级图标：根据 level 动态切换图标和颜色 -->
        <el-icon :class="`risk-icon risk-${risk.level.toLowerCase()}`">
          <component :is="riskIcon(risk.level)" />
        </el-icon>
        <div class="risk-content">
          <!-- 风险等级标签 -->
          <el-tag :type="riskTagType(risk.level)" size="small" class="level-tag">
            {{ riskLevelLabel(risk.level) }}
          </el-tag>
          <!-- 风险条款描述 -->
          <span class="description">{{ risk.description }}</span>
        </div>
      </div>
    </div>
    <!-- 综合风险等级汇总（取所有风险项中的最高等级） -->
    <div class="risk-summary">
      <span>综合风险等级：</span>
      <el-tag :type="overallType" effect="dark">{{ overallLabel }}</el-tag>
    </div>
  </div>
</template>

<script setup>
// 说明：合同风险提示组件，根据传入的风险列表计算综合风险等级并展示
import { computed } from 'vue'

const props = defineProps({
  risks: {
    type: Array,
    default: () => []  // 默认空数组，无风险时组件不渲染
  }
})

/**
 * 计算综合风险等级对应的 Element Plus 类型
 * 存在 HIGH 风险 → danger；存在 MEDIUM → warning；否则 → success
 */
const overallType = computed(() => {
  if (props.risks.some(r => r.level === 'HIGH')) return 'danger'
  if (props.risks.some(r => r.level === 'MEDIUM')) return 'warning'
  return 'success'
})

/**
 * 计算综合风险等级的中文标签
 */
const overallLabel = computed(() => {
  if (props.risks.some(r => r.level === 'HIGH')) return '高风险'
  if (props.risks.some(r => r.level === 'MEDIUM')) return '中等风险'
  return '低风险'
})

/**
 * 将风险等级映射为 Element Plus Tag 的颜色类型
 * @param {string} level - 'HIGH' | 'MEDIUM' | 'LOW'
 */
function riskTagType(level) {
  const map = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'info' }
  return map[level] || 'info'
}

/**
 * 将风险等级枚举映射为中文标签
 */
function riskLevelLabel(level) {
  const map = { HIGH: '高风险', MEDIUM: '中等风险', LOW: '低风险' }
  return map[level] || level
}

/**
 * 根据风险等级返回对应的图标组件名称
 * HIGH → 红色关闭图标；MEDIUM → 橙色警告图标；LOW → 蓝色信息图标
 */
function riskIcon(level) {
  const map = { HIGH: 'CircleCloseFilled', MEDIUM: 'WarnTriangleFilled', LOW: 'InfoFilled' }
  return map[level] || 'InfoFilled'
}
</script>

<style scoped>
/* ===== 风险提示容器 ===== */
.risk-warning {
  border: 1px solid #faecd8;  /* 橙色边框，提示注意 */
  border-radius: 8px;
  overflow: hidden;
  margin: 16px 0;
}

.risk-header {
  border-radius: 0;  /* 去除圆角，与容器边框对齐 */
}

/* ===== 风险列表 ===== */
.risk-list {
  padding: 12px 16px;
}

.risk-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.risk-item:last-child {
  border-bottom: none;
}

/* 风险图标 */
.risk-icon {
  font-size: 18px;
  flex-shrink: 0;
  margin-top: 2px;
}

/* 高/中/低风险图标颜色 */
.risk-high   { color: #f56c6c; }
.risk-medium { color: #e6a23c; }
.risk-low    { color: #e6b820; }

.risk-content {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.level-tag {
  flex-shrink: 0;
}

.description {
  font-size: 13px;
  color: #606266;
}

/* ===== 综合风险汇总区域 ===== */
.risk-summary {
  padding: 10px 16px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #606266;
}
</style>

<style scoped>
.risk-warning {
  border: 1px solid #faecd8;
  border-radius: 8px;
  overflow: hidden;
  margin: 16px 0;
}

.risk-header {
  border-radius: 0;
}

.risk-list {
  padding: 12px 16px;
}

.risk-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.risk-item:last-child {
  border-bottom: none;
}

.risk-icon {
  font-size: 18px;
  flex-shrink: 0;
  margin-top: 2px;
}

.risk-high {
  color: #f56c6c;
}

.risk-medium {
  color: #e6a23c;
}

.risk-low {
  color: #e6b820;
}

.risk-content {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.level-tag {
  flex-shrink: 0;
}

.description {
  font-size: 13px;
  color: #606266;
}

.risk-summary {
  padding: 10px 16px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #606266;
}
</style>
