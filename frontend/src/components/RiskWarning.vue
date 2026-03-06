<template>
  <div class="risk-warning" v-if="risks && risks.length > 0">
    <el-alert
      :title="`合同风险提示（共 ${risks.length} 项风险）`"
      :type="overallType"
      :closable="false"
      show-icon
      class="risk-header"
    />
    <div class="risk-list">
      <div
        v-for="(risk, index) in risks"
        :key="index"
        class="risk-item"
      >
        <el-icon :class="`risk-icon risk-${risk.level.toLowerCase()}`">
          <component :is="riskIcon(risk.level)" />
        </el-icon>
        <div class="risk-content">
          <el-tag :type="riskTagType(risk.level)" size="small" class="level-tag">
            {{ riskLevelLabel(risk.level) }}
          </el-tag>
          <span class="description">{{ risk.description }}</span>
        </div>
      </div>
    </div>
    <div class="risk-summary">
      <span>综合风险等级：</span>
      <el-tag :type="overallType" effect="dark">{{ overallLabel }}</el-tag>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  risks: {
    type: Array,
    default: () => []
  }
})

const overallType = computed(() => {
  if (props.risks.some(r => r.level === 'HIGH')) return 'danger'
  if (props.risks.some(r => r.level === 'MEDIUM')) return 'warning'
  return 'success'
})

const overallLabel = computed(() => {
  if (props.risks.some(r => r.level === 'HIGH')) return '高风险'
  if (props.risks.some(r => r.level === 'MEDIUM')) return '中等风险'
  return '低风险'
})

function riskTagType(level) {
  const map = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'info' }
  return map[level] || 'info'
}

function riskLevelLabel(level) {
  const map = { HIGH: '高风险', MEDIUM: '中等风险', LOW: '低风险' }
  return map[level] || level
}

function riskIcon(level) {
  const map = { HIGH: 'CircleCloseFilled', MEDIUM: 'WarnTriangleFilled', LOW: 'InfoFilled' }
  return map[level] || 'InfoFilled'
}
</script>

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
