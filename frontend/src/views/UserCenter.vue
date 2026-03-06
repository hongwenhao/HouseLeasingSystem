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
import { ref, reactive, computed, onMounted } from 'vue'
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

const userStore = useUserStore()
const activeTab = ref('profile')
const savingProfile = ref(false)
const changingPwd = ref(false)
const ordersLoading = ref(false)
const contractsLoading = ref(false)
const myOrders = ref([])
const myContracts = ref([])
const messages = ref([])
const profileFormRef = ref(null)
const pwdFormRef = ref(null)

const userInfo = computed(() => userStore.userInfo)

const roleLabel = computed(() => {
  const map = { TENANT: '租客', LANDLORD: '房东', ADMIN: '管理员' }
  return map[userInfo.value.role] || userInfo.value.role || '用户'
})

const profileForm = reactive({
  username: userInfo.value.username,
  phone: userInfo.value.phone,
  email: userInfo.value.email,
  avatarUrl: userInfo.value.avatarUrl
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, val, cb) => {
        if (val !== pwdForm.newPassword) cb(new Error('两次密码不一致'))
        else cb()
      },
      trigger: 'blur'
    }
  ]
}

onMounted(async () => {
  try {
    await userStore.fetchProfile()
    Object.assign(profileForm, {
      username: userInfo.value.username,
      phone: userInfo.value.phone,
      email: userInfo.value.email,
      avatarUrl: userInfo.value.avatarUrl
    })
  } catch (e) { /* ignore */ }

  loadOrders()
  loadContracts()
  loadMessages()
})

async function loadOrders() {
  ordersLoading.value = true
  try {
    const res = await getMyOrders({ page: 1, pageSize: 20 })
    myOrders.value = Array.isArray(res) ? res : (res?.list || [])
  } catch (e) { /* ignore */ }
  finally { ordersLoading.value = false }
}

async function loadContracts() {
  contractsLoading.value = true
  try {
    const res = await getMyContracts({ page: 1, pageSize: 20 })
    myContracts.value = Array.isArray(res) ? res : (res?.list || [])
  } catch (e) { /* ignore */ }
  finally { contractsLoading.value = false }
}

async function loadMessages() {
  try {
    const res = await getMessages({ page: 1, pageSize: 50 })
    messages.value = Array.isArray(res) ? res : (res?.list || [])
  } catch (e) { /* ignore */ }
}

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

async function changePassword() {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return
  changingPwd.value = true
  try {
    await changePasswordApi({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    await userStore.logout()
  } catch (e) {
    ElMessage.error(e.message || '密码修改失败')
  } finally {
    changingPwd.value = false
  }
}

async function cancelMyOrder(id) {
  try {
    await cancelOrder(id)
    ElMessage.success('已取消预约')
    loadOrders()
  } catch (e) {
    ElMessage.error(e.message || '取消失败')
  }
}

async function handleMarkRead(id) {
  try {
    await markRead(id)
    const msg = messages.value.find(m => m.id === id)
    if (msg) msg.isRead = true
  } catch (e) { /* ignore */ }
}

async function markAllMessagesRead() {
  try {
    await markAllRead()
    messages.value.forEach(m => m.isRead = true)
    ElMessage.success('已全部标记已读')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

function orderStatusLabel(status) {
  const map = { PENDING: '待确认', CONFIRMED: '已确认', REJECTED: '已拒绝', CANCELLED: '已取消', COMPLETED: '已完成' }
  return map[status] || status
}

function orderStatusType(status) {
  const map = { PENDING: 'warning', CONFIRMED: 'success', REJECTED: 'danger', CANCELLED: 'info', COMPLETED: 'primary' }
  return map[status] || 'info'
}

function contractStatusLabel(status) {
  const map = { PENDING: '待签署', ACTIVE: '生效中', TERMINATED: '已终止', EXPIRED: '已到期' }
  return map[status] || status
}

function contractStatusType(status) {
  const map = { PENDING: 'warning', ACTIVE: 'success', TERMINATED: 'danger', EXPIRED: 'info' }
  return map[status] || 'info'
}

function creditColor(score) {
  if (score >= 90) return '#67c23a'
  if (score >= 70) return '#409eff'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
}

function creditLabel(score) {
  if (score >= 90) return '优秀信用'
  if (score >= 70) return '良好信用'
  if (score >= 60) return '一般信用'
  return '信用较低'
}

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
  background: #f5f7fa;
}

.page-content {
  flex: 1;
  padding: 24px 20px;
}

.page-inner {
  max-width: 900px;
  margin: 0 auto;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 20px;
}

.center-tabs {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
}

.profile-section {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 20px;
}

.avatar-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  align-self: center;
  width: 200px;
}

.profile-form {
  width: 100%;
  max-width: 500px;
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
  background: linear-gradient(135deg, #409eff, #1a6ebd);
  border-radius: 12px;
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
