<template>
  <div class="user-center-page">
    <NavBar />
    <div class="page-content">
      <div class="page-inner">
        <div class="page-header">
          <div>
            <p class="breadcrumb">首页 / 个人中心</p>
            <h2 class="page-title">个人中心</h2>
          </div>
        </div>

        <div class="stats-row">
          <div class="stat-card">
            <div class="stat-title">总预约</div>
            <div class="stat-number primary">{{ myOrders.length }}</div>
            <div class="stat-desc">全部预约记录</div>
          </div>
          <div class="stat-card">
            <div class="stat-title">合同总数</div>
            <div class="stat-number warning">{{ myContracts.length }}</div>
            <div class="stat-desc">所有合同记录</div>
          </div>
          <div class="stat-card">
            <div class="stat-title">未读消息</div>
            <div class="stat-number success">{{ unreadMessages }}</div>
            <div class="stat-desc">消息中心未读</div>
          </div>
          <!-- 信用分快速概览，详情见“信用评分”标签页 -->
          <div class="stat-card">
            <div class="stat-title">信用评分</div>
            <div class="stat-number" :class="creditStatClass">{{ creditScore }}</div>
            <div class="stat-desc">保持良好信用</div>
          </div>
        </div>

        <div class="content-card">
          <el-tabs v-model="activeTab" class="center-tabs" stretch>
            <el-tab-pane name="profile">
              <template #label>
                <div class="tab-label">
                  <el-icon><UserFilled /></el-icon>
                  <span>个人信息</span>
                </div>
              </template>
              <!-- Profile Tab -->
              <div class="section-title">账户信息</div>
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
            <el-tab-pane name="orders">
              <template #label>
                <div class="tab-label">
                  <el-icon><Calendar /></el-icon>
                  <span>预约管理</span>
                </div>
              </template>
              <div v-if="ordersLoading">
                <el-skeleton :rows="4" animated />
              </div>
              <div v-else-if="myOrders.length > 0" class="table-card orders-table">
                <div class="table-head">
                  <span>预约房源</span>
                  <span>预约时间</span>
                  <span>创建时间</span>
                  <span>状态</span>
                  <span>操作</span>
                </div>
                <div
                  v-for="order in myOrders"
                  :key="order.id"
                  class="table-row"
                >
                  <span class="title-cell">{{ order.houseTitle || `房源#${order.houseId}` }}</span>
                  <span>{{ formatDate(order.appointmentDate) }}</span>
                  <span>{{ formatDate(order.createdAt) }}</span>
                  <span>
                    <el-tag :type="orderStatusType(order.status)" size="small">
                      {{ orderStatusLabel(order.status) }}
                    </el-tag>
                  </span>
                  <div class="row-actions">
                    <el-button size="small" @click="$router.push(`/orders/${order.id}`)">查看</el-button>
                    <el-button
                      size="small"
                      type="danger"
                      v-if="order.status === 'PENDING'"
                      @click="cancelMyOrder(order.id)"
                    >取消</el-button>
                  </div>
                </div>
              </div>
              <el-empty v-else description="暂无预约记录" />
            </el-tab-pane>

            <!-- Favorites Tab -->
            <el-tab-pane v-if="isTenant" name="favorites">
              <template #label>
                <div class="tab-label">
                  <el-icon><Star /></el-icon>
                  <span>我的收藏</span>
                </div>
              </template>
              <div v-if="collectionsLoading">
                <el-skeleton :rows="4" animated />
              </div>
              <div v-else-if="myCollections.length > 0" class="favorites-grid">
                <HouseCard
                  v-for="house in myCollections"
                  :key="house.id"
                  :house="house"
                />
              </div>
              <el-empty v-else description="暂无收藏房源" />
            </el-tab-pane>

            <!-- Contracts Tab -->
            <el-tab-pane name="contracts">
              <template #label>
                <div class="tab-label">
                  <el-icon><Memo /></el-icon>
                  <span>合同管理</span>
                </div>
              </template>
              <div v-if="contractsLoading">
                <el-skeleton :rows="4" animated />
              </div>
              <div v-else-if="myContracts.length > 0" class="table-card contracts-table">
                <div class="table-head">
                  <span>合同编号</span>
                  <span>租期</span>
                  <span>月租</span>
                  <span>状态</span>
                  <span>操作</span>
                </div>
                <div
                  v-for="contract in myContracts"
                  :key="contract.id"
                  class="table-row"
                >
                  <span class="title-cell">{{ contract.contractNo || contract.id }}</span>
                  <span>{{ formatDate(contract.startDate) }} 至 {{ formatDate(contract.endDate) }}</span>
                  <span>¥{{ contract.rent }}</span>
                  <span>
                    <el-tag :type="contractStatusType(contract.status)" size="small">
                      {{ contractStatusLabel(contract.status) }}
                    </el-tag>
                  </span>
                  <div class="row-actions">
                    <el-button size="small" @click="$router.push(`/contracts/${contract.id}`)">查看</el-button>
                  </div>
                </div>
              </div>
              <el-empty v-else description="暂无合同记录" />
            </el-tab-pane>

            <!-- Messages Tab -->
            <el-tab-pane name="messages">
              <template #label>
                <div class="tab-label">
                  <el-icon><ChatLineSquare /></el-icon>
                  <span>消息中心</span>
                </div>
              </template>
              <div class="messages-toolbar">
                <el-button size="small" @click="markAllMessagesRead">全部标记已读</el-button>
              </div>
              <MessageList :messages="messages" @read="handleMarkRead" />
            </el-tab-pane>

            <!-- Credit Tab -->
            <el-tab-pane name="credit">
              <template #label>
                <div class="tab-label">
                  <el-icon><StarFilled /></el-icon>
                  <span>信用评分</span>
                </div>
              </template>
              <div class="credit-section">
                <div class="credit-score-card">
                  <div class="score-display">
                    <span class="score-num">{{ creditScore }}</span>
                    <span class="score-total">/100</span>
                  </div>
                  <el-progress
                    :percentage="creditScore"
                    :color="creditColor(creditScore)"
                    :stroke-width="16"
                    class="credit-progress"
                  />
                  <p class="credit-label">{{ creditLabel(creditScore) }}</p>
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
    </div>
    <Footer />
  </div>
</template>

<script setup>
// 说明：个人中心页逻辑，管理用户资料编辑、密码修改、预约订单、合同、消息和信用评分展示
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'     // 用于密码修改后跳转到登录页
import { ElMessage } from 'element-plus'
import { UserFilled, ChatLineSquare, Calendar, Memo, StarFilled, Star } from '@element-plus/icons-vue'
import NavBar from '../components/NavBar.vue'
import Footer from '../components/Footer.vue'
import MessageList from '../components/MessageList.vue'
import HouseCard from '../components/HouseCard.vue'
import { useUserStore } from '../stores/user.js'
import { changePassword as changePasswordApi } from '../api/auth.js'
import { getMyOrders, cancelOrder } from '../api/order.js'
import { getMyContracts } from '../api/contract.js'
import { getMessages, markRead, markAllRead } from '../api/message.js'
import { getMyCollections } from '../api/house.js'

const router = useRouter()                 // 获取路由实例以便在密码修改后跳转
const userStore = useUserStore()
const activeTab = ref('profile')      // 当前激活的 tab 标签名
const savingProfile = ref(false)      // 保存资料按钮 loading 状态
const changingPwd = ref(false)        // 修改密码按钮 loading 状态
const ordersLoading = ref(false)      // 订单列表加载状态
const contractsLoading = ref(false)   // 合同列表加载状态
const collectionsLoading = ref(false) // 收藏列表加载状态
const myOrders = ref([])              // 当前用户的预约订单列表
const myContracts = ref([])           // 当前用户的合同列表
const myCollections = ref([])         // 收藏的房源列表
const messages = ref([])              // 消息通知列表
const profileFormRef = ref(null)
const pwdFormRef = ref(null)

// 从 Pinia store 计算用户信息
const userInfo = computed(() => userStore.userInfo)
const creditScore = computed(() => userInfo.value.creditScore || 100)
const unreadMessages = computed(() => messages.value.filter(m => !m.isRead).length)
const creditStatClass = computed(() => {
  const score = creditScore.value
  if (score >= 90) return 'success'
  if (score >= 70) return 'primary'
  if (score >= 60) return 'warning'
  return 'danger'
})

/** 将角色枚举值映射为中文标签 */
const roleLabel = computed(() => {
  const map = { TENANT: '租客', LANDLORD: '房东', ADMIN: '管理员' }
  return map[userInfo.value.role] || userInfo.value.role || '用户'
})
const isTenant = computed(() => userInfo.value.role === 'TENANT')

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
  loadCollections()
})

/** 加载当前用户的预约订单列表（最多20条） */
async function loadOrders() {
  ordersLoading.value = true
  try {
    const res = await getMyOrders({ page: 1, pageSize: 20 })
    // 后端返回 PageResult 对象，其数据列表字段为 records（非 list）
    myOrders.value = Array.isArray(res) ? res : (res?.records || [])
  } catch (e) { /* ignore */ }
  finally { ordersLoading.value = false }
}

/** 加载当前用户参与的合同列表（最多20条） */
async function loadContracts() {
  contractsLoading.value = true
  try {
    const res = await getMyContracts({ page: 1, pageSize: 20 })
    // 后端返回 PageResult 对象，其数据列表字段为 records（非 list）
    myContracts.value = Array.isArray(res) ? res : (res?.records || [])
  } catch (e) { /* ignore */ }
  finally { contractsLoading.value = false }
}

/** 加载当前用户收藏的房源列表（最多30条） */
async function loadCollections() {
  collectionsLoading.value = true
  try {
    if (!isTenant.value) {
      // 非租客角色不展示收藏，直接清空数据避免无效请求
      myCollections.value = []
      return
    }
    const res = await getMyCollections({ page: 1, pageSize: 30 })
    myCollections.value = Array.isArray(res)
      ? res
      : (res?.records || res?.list || [])
  } catch (e) { /* ignore */ }
  finally { collectionsLoading.value = false }
}

/** 加载当前用户的消息通知列表（最多50条） */
async function loadMessages() {
  try {
    const res = await getMessages({ page: 1, pageSize: 50 })
    // 后端返回 PageResult 对象，其数据列表字段为 records（非 list）
    messages.value = Array.isArray(res) ? res : (res?.records || [])
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
  background: #eef1f6;
  --user-center-text-muted: #9aa3b1;
  --user-center-stat-min: 180px;
  /* profile grid: avatar | forms (1 : 1.5 ratio, form side wider) */
  --profile-avatar-form-cols: 1fr 1.5fr;
  /* table columns */
  --orders-table-cols: 2fr 1.2fr 1.2fr 1fr 1.2fr;       /* title | appointment | created | status | actions */
  --contracts-table-cols: 2fr 1.4fr 1.1fr 1fr 1.2fr;   /* number | lease | rent | status | actions */
  --avatar-area-inset-shadow-alpha: 0.8;
}

.page-content {
  flex: 1;
  padding: 32px 20px 48px;
}

.page-inner {
  max-width: 1100px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.breadcrumb {
  margin: 0 0 6px;
  color: var(--user-center-text-muted);
  font-size: 13px;
}

.page-title {
  margin: 0;
  font-size: 26px;
  font-weight: 800;
  color: #1a1a2e;
  letter-spacing: 0.2px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(var(--user-center-stat-min), 1fr));
  gap: 16px;
  margin: 12px 0 18px;
}

.stat-card {
  background: #fff;
  border-radius: 14px;
  padding: 18px;
  box-shadow: 0 10px 28px rgba(31, 45, 61, 0.05);
  border: 1px solid #edf0f7;
}

.stat-title {
  color: #7a8597;
  font-size: 13px;
  margin-bottom: 6px;
}

.stat-number {
  font-size: 32px;
  font-weight: 800;
  line-height: 1.1;
}

.stat-number.primary { color: #5b6dff; }
.stat-number.warning { color: #ff8f3f; }
.stat-number.success { color: #2eb872; }
.stat-number.danger { color: #e55673; }

.stat-desc {
  color: #9aa3b1;
  font-size: 12px;
  margin-top: 4px;
}

.content-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 12px 32px rgba(31, 45, 61, 0.08);
  border: 1px solid #e8ebf3;
  overflow: hidden;
}

.center-tabs {
  padding: 0 20px 24px;
}

.center-tabs :deep(.el-tabs__header) {
  margin: 0;
  border-bottom: 1px solid #eef0f6;
}

.center-tabs :deep(.el-tabs__nav-wrap)::after {
  display: none;
}

.center-tabs :deep(.el-tabs__item) {
  padding: 16px 22px;
  font-weight: 700;
  color: #7a8597;
  transition: color 0.2s ease;
}

.center-tabs :deep(.is-active) {
  color: #5b6dff;
}

.center-tabs :deep(.el-tabs__active-bar) {
  height: 3px;
  background: linear-gradient(135deg, #667eea, #764ba2);
}

.favorites-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
  padding: 16px 0;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.section-title {
  margin: 20px 0 14px;
  font-size: 16px;
  font-weight: 800;
  color: #1f2d3d;
}

.profile-section {
  display: grid;
  grid-template-columns: var(--profile-avatar-form-cols);
  gap: 24px;
  align-items: start;
}

.avatar-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 26px 20px;
  background: linear-gradient(135deg, #f3f4ff, #f7f8ff);
  border-radius: 14px;
  border: 1px solid #e5e8f3;
  box-shadow: inset 0 1px 0 rgba(255,255,255,var(--avatar-area-inset-shadow-alpha));
}

.avatar-area:hover {
  box-shadow: 0 10px 24px rgba(31, 45, 61, 0.08);
}

.profile-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.table-card {
  border: 1px solid #edf0f7;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 6px 18px rgba(31, 45, 61, 0.04);
}

.table-head,
.table-row {
  display: grid;
  grid-template-columns: var(--orders-table-cols);
  align-items: center;
  padding: 14px 16px;
}

.table-head {
  background: #f7f8fb;
  color: #6c7686;
  font-weight: 700;
  font-size: 13px;
}

.table-row {
  background: #fff;
  border-top: 1px solid #edf0f7;
}

.messages-toolbar {
  margin: 14px 0;
  text-align: right;
}

.contracts-table .table-head,
.contracts-table .table-row {
  grid-template-columns: var(--contracts-table-cols);
}

.title-cell {
  font-weight: 700;
  color: #1f2d3d;
}

.row-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.credit-section {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  background: #f7f8fb;
  padding: 20px;
  border-radius: 12px;
  border: 1px solid #edf0f7;
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

@media (max-width: 900px) {
  .user-center-page {
    --profile-avatar-form-cols: 1fr;
    --user-center-stat-min: 150px;
  }
}
</style>
