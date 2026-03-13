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
/* ===== 404 页面：渐变背景 + 居中内容 ===== */
.not-found-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0f2f5 0%, #e8ecf4 50%, #dce3ed 100%);
  padding: 20px;
}

.not-found-content {
  text-align: center;
  animation: fadeIn 0.6s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 大号 404 错误码：渐变色文字 + 光影效果 */
.error-code {
  font-size: 140px;
  font-weight: 900;
  line-height: 1;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: none;
  filter: drop-shadow(0 4px 20px rgba(102, 126, 234, 0.3));
}

.error-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 12px;
}

.error-desc {
  font-size: 15px;
  color: #909399;
  margin-bottom: 36px;
}
</style>
