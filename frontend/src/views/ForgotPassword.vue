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
.forgot-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a6ebd 0%, #0f4c8c 50%, #1a8a5e 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.forgot-container {
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  width: 100%;
  max-width: 420px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.forgot-header {
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

.reset-btn {
  width: 100%;
  margin-top: 8px;
  font-size: 16px;
}

.forgot-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: #606266;
}

.login-link {
  color: #409eff;
  text-decoration: none;
  margin-left: 4px;
  font-weight: 500;
}
</style>
