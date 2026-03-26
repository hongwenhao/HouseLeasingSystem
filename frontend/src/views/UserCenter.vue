<template>
  <div class="user-center-page">
    <NavBar />
    <div class="page-content">
      <div class="page-inner">
        <h2 class="page-title">个人中心</h2>
        <el-tabs v-model="activeTab" class="center-tabs">
          <!-- Profile Tab -->
          <el-tab-pane label="个人信息" name="profile">
            <div class="profile-section">
              <div class="avatar-area">
                <el-avatar :size="80" :src="userInfo.avatarUrl || ''" :icon="UserFilled" />
                <h3>{{ userInfo.username }}</h3>
                <el-tag>{{ roleLabel }}</el-tag>
              </div>
              <el-form
                ref="profileFormRef"
                :model="profileForm"
                label-width="100px"
                class="profile-form"
              >
                <el-form-item label="用户名">
                  <el-input v-model="profileForm.username" />
                </el-form-item>
                <el-form-item label="手机号">
                  <el-input v-model="profileForm.phone" />
                </el-form-item>
                <el-form-item label="邮箱">
                  <el-input v-model="profileForm.email" />
                </el-form-item>
                <el-form-item label="头像URL">
                  <el-input v-model="profileForm.avatarUrl" placeholder="输入头像图片链接" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存修改</el-button>
                </el-form-item>
              </el-form>

              <el-divider>修改密码</el-divider>
              <el-form
                ref="pwdFormRef"
                :model="pwdForm"
                :rules="pwdRules"
                label-width="100px"
                class="profile-form"
              >
                <el-form-item label="旧密码" prop="oldPassword">
                  <el-input v-model="pwdForm.oldPassword" type="password" show-password />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                  <el-input v-model="pwdForm.newPassword" type="password" show-password />
                </el-form-item>
                <el-form-item label="确认新密码" prop="confirmPassword">
                  <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
                </el-form-item>
                <el-form-item>
                  <el-button type="warning" :loading="changingPwd" @click="changePassword">修改密码</el-button>
                </el-form-item>
              </el-form>
            </div>
          </el-tab-pane>

          <!-- Orders Tab -->
          <el-tab-pane label="我的预约" name="orders">
            <div v-if="ordersLoading">
              <el-skeleton :rows="4" animated />
            </div>
            <div v-else-if="myOrders.length > 0">
              <div
                v-for="order in myOrders"
                :key="order.id"
                class="order-item"
              >
                <div class="order-info">
                  <span class="order-title">{{ order.houseTitle || `房源#${order.houseId}` }}</span>
                  <el-tag :type="orderStatusType(order.status)" size="small">
                    {{ orderStatusLabel(order.status) }}
                  </el-tag>
                </div>
                <div class="order-meta">
                  <span>预约时间：{{ formatDate(order.appointmentDate) }}</span>
                  <span>创建时间：{{ formatDate(order.createdAt) }}</span>
                </div>
                <div class="order-actions">
                  <el-button size="small" @click="$router.push(`/orders/${order.id}`)">查看详情</el-button>
                  <el-button
                    size="small"
                    type="danger"
                    v-if="order.status === 'PENDING'"
                    @click="cancelMyOrder(order.id)"
                  >取消预约</el-button>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无预约记录" />
          </el-tab-pane>

          <!-- Contracts Tab -->
          <el-tab-pane label="我的合同" name="contracts">
            <div v-if="contractsLoading">
              <el-skeleton :rows="4" animated />
            </div>
            <div v-else-if="myContracts.length > 0">
              <div
                v-for="contract in myContracts"
                :key="contract.id"
                class="contract-item"
              >
                <div class="contract-info">
                  <span class="contract-no">合同编号：{{ contract.contractNo || contract.id }}</span>
                  <el-tag :type="contractStatusType(contract.status)" size="small">
                    {{ contractStatusLabel(contract.status) }}
                  </el-tag>
                </div>
                <div class="contract-meta">
                  <span>租期：{{ formatDate(contract.startDate) }} 至 {{ formatDate(contract.endDate) }}</span>
                  <span>月租：¥{{ contract.rent }}</span>
                </div>
                <el-button size="small" @click="$router.push(`/contracts/${contract.id}`)">查看合同</el-button>
              </div>
            </div>
            <el-empty v-else description="暂无合同记录" />
          </el-tab-pane>

          <!-- Messages Tab -->
          <el-tab-pane label="消息中心" name="messages">
            <div class="messages-toolbar">
              <el-button size="small" @click="markAllMessagesRead">全部标记已读</el-button>
            </div>
            <MessageList :messages="messages" @read="handleMarkRead" />
          </el-tab-pane>

          <!-- Credit Tab -->
          <el-tab-pane label="信用评分" name="credit">
            <div class="credit-section">
              <div class="credit-score-card">
                <div class="score-display">
                  <span class="score-num">{{ userInfo.creditScore || 100 }}</span>
                  <span class="score-total">/100</span>
                </div>
                <el-progress
                  :percentage="userInfo.creditScore || 100"
                  :color="creditColor(userInfo.creditScore)"
                  :stroke-width="16"
                  class="credit-progress"
                />
                <p class="credit-label">{{ creditLabel(userInfo.creditScore) }}</p>
              </div>
              <div class="credit-desc">
                <h4>信用评分说明</h4>
                <p>信用评分反映您在平台上的信誉状况，由交易记录、合同履约情况等综合计算。</p>
                <ul>
                  <li>90-100分：优秀信用，享受优先推荐</li>
                  <li>70-89分：良好信用</li>
                  <li>60-69分：一般信用</li>
                  <li>60分以下：信用较低，部分功能受限</li>
                </ul>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
    <Footer />
  </div>
</template>

<script setup>
// 说明：个人中心页逻辑，管理用户资料编辑、密码修改、预约订单、合同、消息和信用评分展示
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'     // 用于密码修改后跳转到登录页
import { ElMessage } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import NavBar from '../components/NavBar.vue'
import Footer from '../components/Footer.vue'
import MessageList from '../components/MessageList.vue'
import { useUserStore } from '../stores/user.js'
import { changePassword as changePasswordApi } from '../api/auth.js'
import { getMyOrders, cancelOrder } from '../api/order.js'
import { getMyContracts } from '../api/contract.js'
import { getMessages, markRead, markAllRead } from '../api/message.js'

const router = useRouter()                 // 获取路由实例以便在密码修改后跳转
const userStore = useUserStore()
const activeTab = ref('profile')      // 当前激活的 tab 标签名
const savingProfile = ref(false)      // 保存资料按钮 loading 状态
const changingPwd = ref(false)        // 修改密码按钮 loading 状态
const ordersLoading = ref(false)      // 订单列表加载状态
const contractsLoading = ref(false)   // 合同列表加载状态
const myOrders = ref([])              // 当前用户的预约订单列表
const myContracts = ref([])           // 当前用户的合同列表
const messages = ref([])              // 消息通知列表
const profileFormRef = ref(null)
const pwdFormRef = ref(null)

// 从 Pinia store 计算用户信息
const userInfo = computed(() => userStore.userInfo)

/** 将角色枚举值映射为中文标签 */
const roleLabel = computed(() => {
  const map = { TENANT: '租客', LANDLORD: '房东', ADMIN: '管理员' }
  return map[userInfo.value.role] || userInfo.value.role || '用户'
})

// 资料编辑表单（初始值从 store 取）
const profileForm = reactive({
  username: userInfo.value.username,
  phone: userInfo.value.phone,
  email: userInfo.value.email,
  avatarUrl: userInfo.value.avatarUrl
})

// 密码修改表单
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 密码修改表单校验规则
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      // 自定义校验：两次密码必须一致
      validator: (rule, val, cb) => {
        if (val !== pwdForm.newPassword) cb(new Error('两次密码不一致'))
        else cb()
      },
      trigger: 'blur'
    }
  ]
}

onMounted(async () => {
  // 拉取最新用户信息，更新资料表单的初始值
  try {
    await userStore.fetchProfile()
    Object.assign(profileForm, {
      username: userInfo.value.username,
      phone: userInfo.value.phone,
      email: userInfo.value.email,
      avatarUrl: userInfo.value.avatarUrl
    })
  } catch (e) { /* ignore */ }

  // 并发加载订单、合同、消息数据
  loadOrders()
  loadContracts()
  loadMessages()
})

/** 加载当前用户的预约订单列表（最多20条） */
async function loadOrders() {
  ordersLoading.value = true
  try {
    const res = await getMyOrders({ page: 1, pageSize: 20 })
    myOrders.value = Array.isArray(res) ? res : (res?.list || [])
  } catch (e) { /* ignore */ }
  finally { ordersLoading.value = false }
}

/** 加载当前用户参与的合同列表（最多20条） */
async function loadContracts() {
  contractsLoading.value = true
  try {
    const res = await getMyContracts({ page: 1, pageSize: 20 })
    myContracts.value = Array.isArray(res) ? res : (res?.list || [])
  } catch (e) { /* ignore */ }
  finally { contractsLoading.value = false }
}

/** 加载当前用户的消息通知列表（最多50条） */
async function loadMessages() {
  try {
    const res = await getMessages({ page: 1, pageSize: 50 })
    messages.value = Array.isArray(res) ? res : (res?.list || [])
  } catch (e) { /* ignore */ }
}

/** 保存用户资料修改 */
async function saveProfile() {
  savingProfile.value = true
  try {
    await userStore.updateProfile(profileForm)
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    savingProfile.value = false
  }
}

/**
 * 提交密码修改
 * 修改成功后强制登出，要求用户重新登录（使旧 token 失效）
 */
async function changePassword() {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return
  changingPwd.value = true
  try {
    await changePasswordApi({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    await userStore.logout()  // 清除本地登录态
    await router.replace('/login').catch(err => {
      console.error('修改密码后跳转登录页失败：', err) // 记录跳转异常便于排查
      ElMessage.error('跳转登录页失败，请手动重新登录') // 提示用户自行重新登录
    }) // 跳转到登录页并记录潜在导航错误
  } catch (e) {
    ElMessage.error(e.message || '密码修改失败')
  } finally {
    changingPwd.value = false
  }
}

/** 取消指定预约订单 */
async function cancelMyOrder(id) {
  try {
    await cancelOrder(id)
    ElMessage.success('已取消预约')
    loadOrders()  // 重新加载订单列表以更新状态
  } catch (e) {
    ElMessage.error(e.message || '取消失败')
  }
}

/**
 * 将指定消息标记为已读
 * @param {number} id - 消息 ID
 */
async function handleMarkRead(id) {
  try {
    await markRead(id)
    // 本地更新消息已读状态，无需重新请求接口
    const msg = messages.value.find(m => m.id === id)
    if (msg) msg.isRead = true
  } catch (e) { /* ignore */ }
}

/** 将所有消息一键标记为已读 */
async function markAllMessagesRead() {
  try {
    await markAllRead()
    messages.value.forEach(m => m.isRead = true)
    ElMessage.success('已全部标记已读')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

/** 订单状态枚举转中文 */
function orderStatusLabel(status) {
  const map = { PENDING: '待确认', CONFIRMED: '已确认', REJECTED: '已拒绝', CANCELLED: '已取消', COMPLETED: '已完成' }
  return map[status] || status
}

/** 订单状态对应的 Element Plus Tag 类型 */
function orderStatusType(status) {
  const map = { PENDING: 'warning', CONFIRMED: 'success', REJECTED: 'danger', CANCELLED: 'info', COMPLETED: 'primary' }
  return map[status] || 'info'
}

/** 合同状态枚举转中文 */
function contractStatusLabel(status) {
  const map = { PENDING: '待签署', ACTIVE: '生效中', TERMINATED: '已终止', EXPIRED: '已到期' }
  return map[status] || status
}

/** 合同状态对应的 Element Plus Tag 类型 */
function contractStatusType(status) {
  const map = { PENDING: 'warning', ACTIVE: 'success', TERMINATED: 'danger', EXPIRED: 'info' }
  return map[status] || 'info'
}

/**
 * 根据信用分返回对应的进度条颜色
 * 90+ 绿色优秀；70-89 蓝色良好；60-69 橙色一般；60以下 红色较低
 */
function creditColor(score) {
  if (score >= 90) return '#67c23a'
  if (score >= 70) return '#409eff'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
}

/** 根据信用分返回等级描述文字 */
function creditLabel(score) {
  if (score >= 90) return '优秀信用'
  if (score >= 70) return '良好信用'
  if (score >= 60) return '一般信用'
  return '信用较低'
}

/** 格式化日期为本地化中文短日期 */
function formatDate(date) {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.user-center-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
}

.page-content {
  flex: 1;
  padding: 32px 20px;
}

.page-inner {
  max-width: 900px;
  margin: 0 auto;
}

.page-title {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 24px;
  position: relative;
  padding-left: 16px;
}

.page-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 4px;
  border-radius: 2px;
  background: linear-gradient(180deg, #667eea, #764ba2);
}

.center-tabs {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}

.profile-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.avatar-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 24px;
  background: linear-gradient(135deg, rgba(102,126,234,0.06), rgba(118,75,162,0.06));
  border-radius: 16px;
  align-self: center;
  width: 220px;
  transition: box-shadow 0.3s ease;
}

.avatar-area:hover {
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}

.profile-form {
  width: 100%;
  max-width: 500px;
  margin: 0 auto; /* 输入表单整体居中 */
}

.profile-form :deep(.el-form-item) {
  width: 100%; /* 确保每个表单项占满可用宽度 */
  max-width: 500px; /* 与表单容器宽度保持一致便于居中 */
  margin: 0 auto; /* 将表单项居中对齐 */
}

.order-item, .contract-item {
  background: #f9fafb;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  border: 1px solid #ebeef5;
}

.order-info, .contract-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.order-title, .contract-no {
  font-weight: 600;
  color: #303133;
}

.order-meta, .contract-meta {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: #909399;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.order-actions {
  display: flex;
  gap: 8px;
}

.messages-toolbar {
  margin-bottom: 12px;
  text-align: right;
}

.credit-section {
  display: flex;
  gap: 40px;
  flex-wrap: wrap;
}

.credit-score-card {
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 16px;
  padding: 32px;
  color: #fff;
  min-width: 240px;
  text-align: center;
}

.score-display {
  margin-bottom: 16px;
}

.score-num {
  font-size: 64px;
  font-weight: 700;
  line-height: 1;
}

.score-total {
  font-size: 20px;
  opacity: 0.8;
}

.credit-progress {
  margin-bottom: 12px;
}

.credit-label {
  font-size: 16px;
  opacity: 0.9;
}

.credit-desc {
  flex: 1;
}

.credit-desc h4 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #303133;
}

.credit-desc p {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 12px;
}

.credit-desc ul {
  font-size: 13px;
  color: #606266;
  padding-left: 20px;
  line-height: 2;
}
</style>
