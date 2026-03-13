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
  color: #303133;            /* Element Plus 主文字色 */
  background-color: #f0f2f5; /* 页面背景色（更柔和的浅灰） */
  -webkit-font-smoothing: antialiased;   /* 字体抗锯齿渲染 */
  -moz-osx-font-smoothing: grayscale;
}

/* 确保应用至少撑满整个视口高度 */
#app {
  min-height: 100vh;
}

/* ===== 全局滚动条美化 ===== */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-thumb {
  background: rgba(144, 147, 153, 0.3);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(144, 147, 153, 0.5);
}

::-webkit-scrollbar-track {
  background: transparent;
}

/* ===== 全局链接样式优化 ===== */
a {
  text-decoration: none;
  color: inherit;
  transition: color 0.2s;
}

/* ===== Element Plus 全局微调 ===== */
/* 卡片圆角统一增大 */
.el-card {
  border-radius: 12px !important;
  border: none !important;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04) !important;
  transition: box-shadow 0.3s ease !important;
}

.el-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08) !important;
}

/* 按钮圆角统一 */
.el-button {
  border-radius: 8px !important;
  font-weight: 500 !important;
}

/* 输入框圆角统一 */
.el-input__wrapper,
.el-select__wrapper {
  border-radius: 8px !important;
}

/* 标签圆角统一 */
.el-tag {
  border-radius: 6px !important;
}
</style>
