<template>
  <!-- 组件说明：页脚组件，显示品牌名称、版权声明、快捷链接及平台简介。
       固定在页面底部，使用深色背景（#1a1a2e）与页面主体形成视觉对比。 -->
  <footer class="footer">
    <div class="footer-inner">
      <!-- 品牌信息区域 -->
      <div class="footer-brand">
        <span class="brand-name">房屋租赁系统</span>
        <span class="copyright">© 2024 版权所有</span>
      </div>
      <!-- 快捷链接区域 -->
      <div class="footer-links">
        <a href="#" class="footer-link">关于我们</a>
        <a href="#" class="footer-link">联系我们</a>
        <a href="#" class="footer-link">使用条款</a>
        <a href="#" class="footer-link">隐私政策</a>
      </div>
      <!-- 平台简介 -->
      <div class="footer-desc">
        <p>专业的房屋租赁平台，连接房东与租客，安全可信赖</p>
      </div>
    </div>
  </footer>
</template>

<script setup>
// 说明：页脚组件，无逻辑，仅展示固定内容
</script>

<style scoped>
/* ===== 页脚容器 ===== */
.footer {
  background: #1a1a2e;  /* 深蓝黑色背景 */
  color: #a0a0b0;        /* 淡灰色文字 */
  padding: 32px 20px;
  margin-top: auto;      /* flex 布局中自动推到底部 */
}

.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  text-align: center;
}

/* ===== 品牌信息 ===== */
.footer-brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.brand-name {
  font-size: 20px;
  font-weight: 700;
  color: #fff;  /* 品牌名使用白色强调 */
}

.copyright {
  font-size: 13px;
}

/* ===== 快捷链接 ===== */
.footer-links {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  justify-content: center;
}

.footer-link {
  color: #a0a0b0;
  text-decoration: none;
  font-size: 14px;
  transition: color 0.2s;
}

.footer-link:hover {
  color: #409eff;  /* 悬停变主色蓝 */
}

/* ===== 平台简介 ===== */
.footer-desc {
  font-size: 13px;
  color: #6a6a7a;
}
</style>

<style scoped>
.footer {
  background: #1a1a2e;
  color: #a0a0b0;
  padding: 32px 20px;
  margin-top: auto;
}

.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  text-align: center;
}

.footer-brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.brand-name {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
}

.copyright {
  font-size: 13px;
}

.footer-links {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  justify-content: center;
}

.footer-link {
  color: #a0a0b0;
  text-decoration: none;
  font-size: 14px;
  transition: color 0.2s;
}

.footer-link:hover {
  color: #409eff;
}

.footer-desc {
  font-size: 13px;
  color: #6a6a7a;
}
</style>
