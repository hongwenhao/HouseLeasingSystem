<template>
  <!-- 组件说明：404 页面未找到视图，在用户访问不存在的路由时展示。
       显示大号 404 错误码、提示文案，以及返回首页按钮。
       背景使用渐变色（浅灰到蓝灰），营造平静的视觉效果。 -->
  <div class="not-found-page">
    <div class="not-found-content">
      <!-- 大号 404 错误码 -->
      <div class="error-code">404</div>
      <h2 class="error-title">页面不存在</h2>
      <p class="error-desc">您访问的页面不存在或已被删除，请检查链接或返回首页</p>
      <!-- 返回首页按钮 -->
      <el-button type="primary" size="large" @click="$router.push('/')">
        <el-icon><HomeFilled /></el-icon>
        返回首页
      </el-button>
    </div>
  </div>
</template>

<script setup>
// 说明：404 页面，无业务逻辑，仅展示错误提示和返回按钮
</script>

<style scoped>
.not-found-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  padding: 20px;
}

.not-found-content {
  text-align: center;
}

.error-code {
  font-size: 120px;
  font-weight: 900;
  color: #409eff;
  line-height: 1;
  text-shadow: 4px 4px 20px rgba(64, 158, 255, 0.3);
  margin-bottom: 16px;
}

.error-title {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 12px;
}

.error-desc {
  font-size: 15px;
  color: #909399;
  margin-bottom: 32px;
}
</style>
