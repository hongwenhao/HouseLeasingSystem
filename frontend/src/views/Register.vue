<template>
  <!-- 组件说明：用户注册页，包含用户名、手机号、邮箱、密码（含确认）、
       注册身份（租客/房东）和服务条款同意的完整注册表单。
       注册成功后自动跳转到登录页。 -->
  <div class="register-page">
    <div class="register-container">
      <!-- 注册页头部：Logo + 标题 -->
      <div class="register-header">
        <div class="logo-box" role="img" aria-label="房屋租赁系统">🏠</div>
        <h2 class="title">创建账户</h2>
        <p class="subtitle">加入我们，开启便捷租房之旅</p>
      </div>

      <!-- 注册表单 -->
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
      >
        <!-- 用户名 -->
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            size="large"
            clearable
          />
        </el-form-item>

        <div class="form-row">
          <!-- 手机号 -->
          <el-form-item label="手机号" prop="phone">
            <el-input
              v-model="form.phone"
              placeholder="请输入手机号"
              size="large"
              clearable
            />
          </el-form-item>
          <!-- 邮箱 -->
          <el-form-item label="邮箱" prop="email" required>
            <el-input
              v-model="form.email"
              placeholder="请输入邮箱"
              size="large"
              clearable
            />
          </el-form-item>
        </div>

        <div class="form-row">
          <!-- 密码 -->
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              show-password
            />
          </el-form-item>
          <!-- 确认密码 -->
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              size="large"
              show-password
            />
          </el-form-item>
        </div>

        <!-- 注册身份选择：租客 or 房东 -->
        <el-form-item label="我是" prop="role">
          <el-radio-group v-model="form.role" size="large" class="role-group">
            <el-radio-button value="TENANT">🧍 租客（找房）</el-radio-button>
            <el-radio-button value="LANDLORD">🏡 房东（出租）</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <!-- 服务条款同意 -->
        <el-form-item prop="agreed">
          <el-checkbox v-model="form.agreed">
            我已阅读并同意
            <a href="#" class="terms-link">《使用条款》</a>
            和
            <a href="#" class="terms-link">《隐私政策》</a>
          </el-checkbox>
        </el-form-item>

        <!-- 注册按钮 -->
        <el-button
          type="primary"
          size="large"
          class="register-btn"
          :loading="loading"
          @click="handleRegister"
        >
          立即注册
        </el-button>
      </el-form>

      <!-- 跳转登录页链接 -->
      <div class="register-footer">
        <span>已有账户？</span>
        <router-link to="/login" class="login-link">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
// 说明：注册页逻辑，处理多字段表单验证和注册请求
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '../api/auth.js'

const router = useRouter()
const formRef = ref(null)  // 表单实例引用
const loading = ref(false)

// 注册表单数据
const form = reactive({
  username: '',
  phone: '',
  email: '',
  password: '',
  confirmPassword: '',
  role: 'TENANT',  // 默认注册为租客
  agreed: false
})

/**
 * 自定义校验：确认密码必须与密码一致
 */
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

/**
 * 自定义校验：必须勾选同意条款才能注册
 */
const validateAgreed = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请阅读并同意条款'))
  } else {
    callback()
  }
}

// 表单校验规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '用户名长度为4-20个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  role: [{ required: true, message: '请选择注册身份', trigger: 'change' }],
  agreed: [{ validator: validateAgreed, trigger: 'change' }]
}

/**
 * 处理注册表单提交
 * 1. 前端表单全量校验
 * 2. 调用注册接口（不传 confirmPassword 和 agreed，这两个字段仅前端使用）
 * 3. 注册成功后跳转到登录页
 */
async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await register({
      username: form.username,
      phone: form.phone,
      email: form.email,
      password: form.password,
      role: form.role
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) {
    ElMessage.error(e.message || '注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ===== 注册页全屏背景：柔和渐变 + 装饰光圈 ===== */
.register-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #0f766e 0%, #14b8a6 50%, #99f6e4 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  position: relative;
  overflow: hidden;
}

/* 背景装饰光圈 */
.register-page::before {
  content: '';
  position: absolute;
  top: -15%;
  left: -10%;
  width: 500px;
  height: 500px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}

.register-page::after {
  content: '';
  position: absolute;
  bottom: -20%;
  right: -8%;
  width: 450px;
  height: 450px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
}

/* ===== 注册卡片：毛玻璃效果 + 入场动画 ===== */
.register-container {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 44px 38px 40px;
  width: 100%;
  max-width: 430px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15), 0 0 0 1px rgba(255, 255, 255, 0.2);
  position: relative;
  z-index: 1;
  animation: slideUp 0.6s ease-out;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

.register-header {
  text-align: center;
  margin-bottom: 24px;
}

.logo-box {
  width: 56px;
  height: 56px;
  margin: 0 auto 16px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.3);
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

.form-row {
  display: flex;
  gap: 12px;
}

.form-row .el-form-item {
  flex: 1;
}

/* ===== 角色选择按钮组 ===== */
.role-group {
  width: 100%;
  display: flex;
}

.role-group :deep(.el-radio-button) {
  flex: 1;
}

.role-group :deep(.el-radio-button__inner) {
  width: 100%;
  border-radius: 10px;
}

.role-group :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: #eef2ff;
  border-color: #667eea;
  color: #4f63d8;
  box-shadow: none;
}

.register-container :deep(.el-input__wrapper) {
  border-radius: 10px;
}

.register-container :deep(.el-form-item) {
  margin-bottom: 18px;
}

.terms-link {
  color: #667eea;
  text-decoration: none;
  font-weight: 500;
}

/* ===== 注册按钮：渐变背景 + 悬停上浮 ===== */
.register-btn {
  width: 100%;
  margin-top: 12px;
  font-size: 16px;
  height: 44px;
  background: linear-gradient(135deg, #667eea, #764ba2) !important;
  border: none !important;
  letter-spacing: 2px;
  transition: all 0.3s ease !important;
}

.register-btn:hover {
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4) !important;
  transform: translateY(-1px) !important;
}

.register-footer {
  text-align: center;
  margin-top: 20px;
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
