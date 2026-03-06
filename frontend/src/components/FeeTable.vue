<template>
  <div class="fee-table">
    <el-descriptions title="五项费用说明" :column="1" border>
      <el-descriptions-item label="水费">
        <span>{{ feeTypeLabel(fees.waterFee) }}</span>
        <span class="amount">{{ feeAmount(fees.waterFee, '元/度') }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="电费">
        <span>{{ feeTypeLabel(fees.electricFee) }}</span>
        <span class="amount">{{ feeAmount(fees.electricFee, '元/度') }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="燃气费">
        <span>{{ feeTypeLabel(fees.gasFee) }}</span>
        <span class="amount">{{ feeAmount(fees.gasFee, '元/方') }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="物业费">
        <span>{{ feeTypeLabel(fees.propertyFee) }}</span>
        <span class="amount">{{ feeAmount(fees.propertyFee, '元/月') }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="网络费">
        <span>{{ feeTypeLabel(fees.internetFee) }}</span>
        <span class="amount">{{ feeAmount(fees.internetFee, '元/月') }}</span>
      </el-descriptions-item>
    </el-descriptions>
  </div>
</template>

<script setup>
defineProps({
  fees: {
    type: Object,
    default: () => ({})
  }
})

function feeTypeLabel(fee) {
  if (!fee) return '-'
  const map = { METERED: '计量收费', FIXED: '固定月费', INCLUDED: '房租已含' }
  return map[fee.type] || fee.type || '-'
}

function feeAmount(fee, unit) {
  if (!fee) return ''
  if (fee.type === 'INCLUDED') return ''
  if (fee.amount !== undefined && fee.amount !== null) {
    return `${fee.amount} ${unit}`
  }
  return ''
}
</script>

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
