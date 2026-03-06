<template>
  <!-- 组件说明：五项费用说明表格组件，展示房源的水/电/燃气/物业/网络费用类型和金额。
       费用类型：计量收费（METERED）/ 固定月费（FIXED）/ 房租已含（INCLUDED）。
       若费用已含在房租中，则不显示具体金额。 -->
  <div class="fee-table">
    <el-descriptions title="五项费用说明" :column="1" border>
      <!-- 水费 -->
      <el-descriptions-item label="水费">
        <span>{{ feeTypeLabel(fees.waterFee) }}</span>
        <span class="amount">{{ feeAmount(fees.waterFee, '元/度') }}</span>
      </el-descriptions-item>
      <!-- 电费 -->
      <el-descriptions-item label="电费">
        <span>{{ feeTypeLabel(fees.electricFee) }}</span>
        <span class="amount">{{ feeAmount(fees.electricFee, '元/度') }}</span>
      </el-descriptions-item>
      <!-- 燃气费 -->
      <el-descriptions-item label="燃气费">
        <span>{{ feeTypeLabel(fees.gasFee) }}</span>
        <span class="amount">{{ feeAmount(fees.gasFee, '元/方') }}</span>
      </el-descriptions-item>
      <!-- 物业费 -->
      <el-descriptions-item label="物业费">
        <span>{{ feeTypeLabel(fees.propertyFee) }}</span>
        <span class="amount">{{ feeAmount(fees.propertyFee, '元/月') }}</span>
      </el-descriptions-item>
      <!-- 网络费 -->
      <el-descriptions-item label="网络费">
        <span>{{ feeTypeLabel(fees.internetFee) }}</span>
        <span class="amount">{{ feeAmount(fees.internetFee, '元/月') }}</span>
      </el-descriptions-item>
    </el-descriptions>
  </div>
</template>

<script setup>
// 说明：五项费用表格组件，将费用配置对象转换为可读的文字描述
defineProps({
  fees: {
    type: Object,
    default: () => ({})  // 默认传入空对象，避免渲染报错
  }
})

/**
 * 将费用类型枚举转换为中文说明
 * @param {Object} fee - 单项费用配置 { type: 'METERED'|'FIXED'|'INCLUDED', amount: number }
 */
function feeTypeLabel(fee) {
  if (!fee) return '-'
  const map = { METERED: '计量收费', FIXED: '固定月费', INCLUDED: '房租已含' }
  return map[fee.type] || fee.type || '-'
}

/**
 * 格式化费用金额显示
 * @param {Object} fee    - 单项费用配置对象
 * @param {string} unit   - 金额单位（如 '元/度'、'元/月'）
 * @returns {string}      - 若费用已含则返回空字符串，否则返回 "金额 单位" 字符串
 */
function feeAmount(fee, unit) {
  if (!fee) return ''
  if (fee.type === 'INCLUDED') return '' // 已含在房租中，不展示金额
  if (fee.amount !== undefined && fee.amount !== null) {
    return `${fee.amount} ${unit}`
  }
  return ''
}
</script>

<style scoped>
/* ===== 费用表格容器 ===== */
.fee-table {
  margin: 16px 0;
}

/* 费用金额：红色强调显示 */
.amount {
  margin-left: 16px;
  color: #f56c6c;
  font-weight: 500;
}
</style>

<style scoped>
.fee-table {
  margin: 16px 0;
}

.amount {
  margin-left: 16px;
  color: #f56c6c;
  font-weight: 500;
}
</style>
