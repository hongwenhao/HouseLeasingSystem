<template>
  <!-- 组件说明：房东类型角标组件，根据 ownerType 属性渲染不同颜色的标签。
       OWNER（一手房东）→ 绿色成功标签
       SUBLEASE（二手房东）→ 橙色警告标签
       AGENT（持牌中介）→ 蓝色主色标签 -->
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
// 说明：房东类型角标组件，仅接收 ownerType 属性并渲染对应的标签样式，无业务逻辑
defineProps({
  ownerType: {
    type: String,
    default: 'OWNER'  // 默认为一手房东
  }
})
</script>

<style scoped>
/* 组件无额外样式，使用 Element Plus 标签的内置样式 */
</style>
