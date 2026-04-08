<template>
  <!-- 组件说明：用户登录页，包含用户名/手机号 + 密码表单，
       表单验证通过后调用 Pinia userStore.login() 发起登录请求，
       成功后跳转到 redirect 参数指定的页面或首页。 -->
  <div class="login-page">
    <div class="login-container">
      <!-- 登录页头部：Logo + 标题 -->
      <div class="login-header">
        <router-link to="/" class="back-home">
          <img src="../assets/logo.svg" alt="logo" class="logo" />
        </router-link>
        <h2 class="title">登录房屋租赁系统</h2>
        <p class="subtitle">欢迎回来，请登录您的账户</p>
      </div>

      <!-- 登录表单 -->
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleLogin"
      >
        <!-- 用户名/手机号输入框 -->
        <el-form-item label="用户名 / 手机号" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名或手机号"
            prefix-icon="User"
            size="large"
            clearable
          />
        </el-form-item>
        <!-- 密码输入框（支持回车提交） -->
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <!-- 登录按钮 -->
        <el-button
          type="primary"
          size="large"
          class="login-btn"
          :loading="loading"
          @click="handleLogin"
        >
          登录
        </el-button>
      </el-form>

      <!-- 忘记密码链接 -->
      <div class="forgot-password">
        <router-link to="/forgot-password" class="forgot-link">忘记密码？</router-link>
      </div>

      <!-- 跳转注册页链接 -->
      <div class="login-footer">
        <span>还没有账户？</span>
        <router-link to="/register" class="register-link">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
// 说明：登录页逻辑，处理表单验证和登录请求
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user.js'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref(null)  // 表单实例引用，用于调用 validate()
const loading = ref(false)  // 登录按钮 loading 状态

// 表单数据
const form = reactive({
  username: '',
  password: ''
})

// 表单校验规则
const rules = {
  username: [{ required: true, message: '请输入用户名或手机号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ]
}

/**
 * 处理登录表单提交
 * 1. 前端表单校验
 * 2. 调用 userStore.login() 发起登录请求
 * 3. 登录成功后重定向（支持 redirect 参数）
 */
async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    // 支持登录后重定向：如从 /houses 跳转到登录页，登录后返回 /houses
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (e) {
    ElMessage.error(e.message || '登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ===== 登录页全屏背景：渐变色 + 装饰性几何光圈 ===== */
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #0f766e 0%, #14b8a6 50%, #99f6e4 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  position: relative;
  overflow: hidden;
}

/* 背景装饰光圈（右上角） */
.login-page::before {
  content: '';
  position: absolute;
  top: -20%;
  right: -10%;
  width: 500px;
  height: 500px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}

/* 背景装饰光圈（左下角） */
.login-page::after {
  content: '';
  position: absolute;
  bottom: -15%;
  left: -5%;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
}

/* ===== 登录卡片：毛玻璃效果 + 入场动画 ===== */
.login-container {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 48px 40px;
  width: 100%;
  max-width: 420px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15), 0 0 0 1px rgba(255, 255, 255, 0.2);
  position: relative;
  z-index: 1;
  animation: slideUp 0.6s ease-out;
}

/* 卡片入场动画：从下方滑入并渐显 */
@keyframes slideUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.back-home {
  display: inline-block;
  margin-bottom: 16px;
  transition: transform 0.3s ease;
}

.back-home:hover {
  transform: scale(1.1);
}

.logo {
  width: 64px;
  height: 64px;
  filter: drop-shadow(0 4px 8px rgba(64, 158, 255, 0.3));
}

.title {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 8px;
  letter-spacing: 1px;
}

.subtitle {
  font-size: 14px;
  color: #909399;
}

/* ===== 登录按钮：渐变背景 + 悬停上浮 ===== */
.login-btn {
  width: 100%;
  margin-top: 12px;
  font-size: 16px;
  height: 44px;
  background: linear-gradient(135deg, #667eea, #764ba2) !important;
  border: none !important;
  letter-spacing: 2px;
  transition: all 0.3s ease !important;
}

.login-btn:hover {
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4) !important;
  transform: translateY(-1px) !important;
}

/* ===== 忘记密码链接 ===== */
.forgot-password {
  text-align: right;
  margin-top: 8px;
}

.forgot-link {
  color: #909399;
  text-decoration: none;
  font-size: 13px;
  transition: color 0.2s;
}

.forgot-link:hover {
  color: #667eea;
}

/* ===== 页脚区域 ===== */
.login-footer {
  text-align: center;
  margin-top: 28px;
  font-size: 14px;
  color: #606266;
}

.register-link {
  color: #667eea;
  text-decoration: none;
  margin-left: 4px;
  font-weight: 600;
  transition: color 0.2s;
}

.register-link:hover {
  color: #764ba2;
}
</style>
