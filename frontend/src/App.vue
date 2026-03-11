<template>
  <!-- 根组件：仅负责渲染路由视图，全局样式在此定义 -->
  <router-view />
</template>

<script setup>
// 说明：根组件无业务逻辑，仅作为路由出口容器
</script>

<style>
/* ===== 全局基础重置样式 ===== */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

/* 全局字体设置，优先使用 PingFang SC、微软雅黑等中文字体 */
body {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  color: #303133;          /* Element Plus 主文字色 */
  background-color: #f5f7fa; /* 页面背景色（浅灰） */
}

/* 确保应用至少撑满整个视口高度 */
#app {
  min-height: 100vh;
}
</style>
