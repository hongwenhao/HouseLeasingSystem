<template>
  <!-- 组件说明：用户注册页，包含用户名、手机号、邮箱、密码（含确认）、
       注册身份（租客/房东）和服务条款同意的完整注册表单。
       注册成功后自动跳转到登录页。 -->
  <div class="register-page">
    <div class="register-container">
      <!-- 注册页头部：Logo + 标题 -->
      <div class="register-header">
        <router-link to="/" class="back-home">
          <img src="../assets/logo.svg" alt="logo" class="logo" />
        </router-link>
        <h2 class="title">创建账户</h2>
        <p class="subtitle">加入房屋租赁系统，开启智能租房体验</p>
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
            placeholder="请输入用户名（4-20个字符）"
            prefix-icon="User"
            size="large"
            clearable
          />
        </el-form-item>
        <!-- 手机号 -->
        <el-form-item label="手机号" prop="phone">
          <el-input
            v-model="form.phone"
            placeholder="请输入手机号"
            prefix-icon="Phone"
            size="large"
            clearable
          />
        </el-form-item>
        <!-- 邮箱（可选） -->
        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="form.email"
            placeholder="请输入邮箱（可选）"
            prefix-icon="Message"
            size="large"
            clearable
          />
        </el-form-item>
        <!-- 密码 -->
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码（至少6位）"
            prefix-icon="Lock"
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
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>
        <!-- 注册身份选择：租客 or 房东 -->
        <el-form-item label="注册身份" prop="role">
          <el-radio-group v-model="form.role" size="large" class="role-group">
            <el-radio-button value="TENANT">🏠 我是租客</el-radio-button>
            <el-radio-button value="LANDLORD">🔑 我是房东</el-radio-button>
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
          注册
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
.register-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a6ebd 0%, #0f4c8c 50%, #1a8a5e 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.register-container {
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  width: 100%;
  max-width: 460px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.register-header {
  text-align: center;
  margin-bottom: 28px;
}

.back-home {
  display: inline-block;
  margin-bottom: 12px;
}

.logo {
  width: 48px;
  height: 48px;
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

.role-group {
  width: 100%;
  display: flex;
}

.role-group :deep(.el-radio-button) {
  flex: 1;
}

.role-group :deep(.el-radio-button__inner) {
  width: 100%;
}

.terms-link {
  color: #409eff;
  text-decoration: none;
}

.register-btn {
  width: 100%;
  margin-top: 8px;
  font-size: 16px;
}

.register-footer {
  text-align: center;
  margin-top: 20px;
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
