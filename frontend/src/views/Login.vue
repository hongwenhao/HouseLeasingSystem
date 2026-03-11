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
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a6ebd 0%, #0f4c8c 50%, #1a8a5e 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.login-container {
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  width: 100%;
  max-width: 420px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.back-home {
  display: inline-block;
  margin-bottom: 12px;
}

.logo {
  width: 56px;
  height: 56px;
}

.title {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 6px;
}

.subtitle {
  font-size: 14px;
  color: #909399;
}

.login-btn {
  width: 100%;
  margin-top: 8px;
  font-size: 16px;
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: #606266;
}

.register-link {
  color: #409eff;
  text-decoration: none;
  margin-left: 4px;
  font-weight: 500;
}
</style>
