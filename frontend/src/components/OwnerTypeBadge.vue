<template>
  <el-tag
    v-if="ownerType === 'OWNER'"
    type="success"
    size="small"
    effect="dark"
  >一手房东</el-tag>
  <el-tag
    v-else-if="ownerType === 'SUBLEASE'"
    type="warning"
    size="small"
    effect="dark"
  >二手房东</el-tag>
  <el-tag
    v-else-if="ownerType === 'AGENT'"
    type="primary"
    size="small"
    effect="dark"
  >持牌中介</el-tag>
</template>

<script setup>
defineProps({
  ownerType: {
    type: String,
    default: 'OWNER'
  }
})
</script>

<style scoped>
</style>
