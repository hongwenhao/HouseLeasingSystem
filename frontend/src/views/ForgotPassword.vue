<template>
  <!-- 组件说明：忘记密码页，用户通过用户名和手机号验证身份后重置密码。
       验证通过后更新密码，成功后跳转到登录页。 -->
  <div class="forgot-page">
    <div class="forgot-container">
      <!-- 页头：Logo + 标题 -->
      <div class="forgot-header">
        <router-link to="/" class="back-home">
          <img src="../assets/logo.svg" alt="logo" class="logo" />
        </router-link>
        <h2 class="title">忘记密码</h2>
        <p class="subtitle">请输入您的账户信息以重置密码</p>
      </div>

      <!-- 重置密码表单 -->
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleReset"
      >
        <!-- 用户名输入框 -->
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
            clearable
          />
        </el-form-item>
        <!-- 手机号输入框 -->
        <el-form-item label="注册手机号" prop="phone">
          <el-input
            v-model="form.phone"
            placeholder="请输入注册时绑定的手机号"
            prefix-icon="Phone"
            size="large"
            clearable
          />
        </el-form-item>
        <!-- 新密码输入框 -->
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            placeholder="请输入新密码（至少6位）"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>
        <!-- 确认新密码输入框 -->
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleReset"
          />
        </el-form-item>

        <!-- 重置密码按钮 -->
        <el-button
          type="primary"
          size="large"
          class="reset-btn"
          :loading="loading"
          @click="handleReset"
        >
          重置密码
        </el-button>
      </el-form>

      <!-- 返回登录页链接 -->
      <div class="forgot-footer">
        <span>想起密码了？</span>
        <router-link to="/login" class="login-link">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
// 说明：忘记密码页逻辑，处理表单验证和密码重置请求
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { resetPassword } from '../api/auth.js'

const router = useRouter()
const formRef = ref(null)  // 表单实例引用
const loading = ref(false)  // 按钮 loading 状态

// 表单数据
const form = reactive({
  username: '',
  phone: '',
  newPassword: '',
  confirmPassword: ''
})

/**
 * 自定义校验：确认密码必须与新密码一致
 */
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 表单校验规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入注册手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

/**
 * 处理重置密码表单提交
 * 1. 前端表单校验
 * 2. 调用重置密码接口
 * 3. 成功后跳转到登录页
 */
async function handleReset() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await resetPassword({
      username: form.username,
      phone: form.phone,
      newPassword: form.newPassword
    })
    ElMessage.success('密码重置成功，请使用新密码登录')
    router.push('/login')
  } catch (e) {
    ElMessage.error(e.message || '密码重置失败，请检查信息是否正确')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ===== 忘记密码页背景：与登录页统一的渐变风格 ===== */
.forgot-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.forgot-page::before {
  content: '';
  position: absolute;
  top: -20%;
  right: -10%;
  width: 500px;
  height: 500px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}

/* ===== 表单卡片：毛玻璃 + 入场动画 ===== */
.forgot-container {
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

@keyframes slideUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

.forgot-header {
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

/* ===== 重置密码按钮 ===== */
.reset-btn {
  width: 100%;
  margin-top: 12px;
  font-size: 16px;
  height: 44px;
  background: linear-gradient(135deg, #667eea, #764ba2) !important;
  border: none !important;
  letter-spacing: 2px;
  transition: all 0.3s ease !important;
}

.reset-btn:hover {
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4) !important;
  transform: translateY(-1px) !important;
}

.forgot-footer {
  text-align: center;
  margin-top: 28px;
  font-size: 14px;
  color: #606266;
}

.login-link {
  color: #667eea;
  text-decoration: none;
  margin-left: 4px;
  font-weight: 600;
  transition: color 0.2s;
}

.login-link:hover {
  color: #764ba2;
}
</style>
