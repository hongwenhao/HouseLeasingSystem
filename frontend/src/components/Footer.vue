<template>
  <footer class="footer">
    <div class="footer-inner">
      <div class="footer-brand">
        <span class="brand-name">房屋租赁系统</span>
        <span class="copyright">© 2024 版权所有</span>
      </div>
      <div class="footer-links">
        <a href="#" class="footer-link">关于我们</a>
        <a href="#" class="footer-link">联系我们</a>
        <a href="#" class="footer-link">使用条款</a>
        <a href="#" class="footer-link">隐私政策</a>
      </div>
      <div class="footer-desc">
        <p>专业的房屋租赁平台，连接房东与租客，安全可信赖</p>
      </div>
    </div>
  </footer>
</template>

<script setup>
</script>

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
