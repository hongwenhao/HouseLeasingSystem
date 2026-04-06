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
          <div v-if="!isAdmin" class="stat-card">
            <div class="stat-title">总预约</div>
            <div class="stat-number primary">{{ myOrders.length }}</div>
            <div class="stat-desc">全部预约记录</div>
          </div>
          <div v-if="!isAdmin" class="stat-card">
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
                  <el-form-item label="性别">
                    <el-select v-model="profileForm.gender" placeholder="请选择性别">
                      <el-option :value="0" label="保密" />
                      <el-option :value="1" label="男" />
                      <el-option :value="2" label="女" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="头像">
                    <div class="avatar-upload-field">
                      <!--
                        头像上传入口：
                        1) 复用统一图片上传接口，确保鉴权、错误提示、返回结构与全站一致；
                        2) 关闭自动上传，交由 handleAvatarFileChange 做类型校验与状态控制；
                        3) 上传成功后仅回填 profileForm.avatarUrl，不会立即提交资料，避免误保存。
                      -->
                      <el-upload
                        action="#"
                        :auto-upload="false"
                        :show-file-list="false"
                        accept="image/*"
                        :disabled="uploadingAvatar"
                        :on-change="handleAvatarFileChange"
                      >
                        <el-button type="primary" plain :loading="uploadingAvatar">本地上传头像</el-button>
                      </el-upload>
                      <!--
                        按需求移除“头像 URL 地址栏”输入框：
                        用户仅通过上传方式更新头像，减少手动填写链接带来的错误地址/失效地址问题。
                      -->
                    </div>
                  </el-form-item>
                  <el-form-item>
                    <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存修改</el-button>
                  </el-form-item>
                </el-form>

                <el-divider>实名认证</el-divider>
                <el-form :model="realNameForm" label-width="100px" class="profile-form">
                  <el-form-item label="认证状态">
                    <el-tag :type="isRealNameAuth ? 'success' : 'warning'">
                      {{ isRealNameAuth ? '已实名认证' : '未实名认证' }}
                    </el-tag>
                  </el-form-item>
                  <el-form-item label="真实姓名">
                    <el-input v-model="realNameForm.realName" :disabled="isRealNameAuth" placeholder="请输入真实姓名" />
                  </el-form-item>
                  <el-form-item label="身份证号">
                    <el-input v-model="realNameForm.idCard" :disabled="isRealNameAuth" placeholder="请输入身份证号" />
                  </el-form-item>
                  <el-form-item>
                    <el-button
                      type="success"
                      :loading="submittingRealName"
                      :disabled="isRealNameAuth"
                      @click="submitRealNameAuth"
                    >
                      提交实名认证
                    </el-button>
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

            <!-- Favorites Tab -->
            <el-tab-pane v-if="isTenant" name="favorites">
              <template #label>
                <div class="tab-label">
                  <el-icon><Star /></el-icon>
                  <span>我的收藏</span>
                </div>
              </template>
              <div class="table-toolbar">
                <!-- 收藏搜索栏：按房源标题、地址、租金等关键字进行前端筛选 -->
                <el-input
                  v-model.trim="collectionSearchKeyword"
                  class="search-input"
                  clearable
                  placeholder="搜索收藏（标题/地址/租金）"
                />
              </div>
              <div v-if="collectionsLoading">
                <el-skeleton :rows="4" animated />
              </div>
              <div v-else-if="filteredMyCollections.length > 0" class="favorites-grid">
                <HouseCard
                  v-for="house in filteredMyCollections"
                  :key="house.id"
                  :house="house"
                />
              </div>
              <el-empty v-else description="暂无收藏房源" />
            </el-tab-pane>

            <!-- Orders Tab -->
            <el-tab-pane v-if="!isAdmin" name="orders">
              <template #label>
                <div class="tab-label">
                  <el-icon><Calendar /></el-icon>
                  <span>预约订单管理</span>
                </div>
              </template>
              <div class="table-toolbar">
                <!-- 预约订单搜索栏：仅按文本字段搜索，状态/支付/时间改为右侧下拉筛选 -->
                <el-input
                  v-model.trim="orderSearchKeyword"
                  class="search-input"
                  clearable
                  placeholder="搜索预约订单（订单号/房源）"
                />
                <!-- 订单筛选区：将状态、支付状态、时间维度独立成下拉框，减少关键字搜索噪音 -->
                <div class="toolbar-filters">
                  <el-select v-model="orderStatusFilter" clearable placeholder="订单状态" class="filter-select">
                    <el-option label="全部订单状态" value="" />
                    <el-option
                      v-for="option in orderStatusFilterOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                  <el-select v-model="paymentStatusFilter" clearable placeholder="支付状态" class="filter-select">
                    <el-option label="全部支付状态" value="" />
                    <el-option
                      v-for="option in paymentStatusFilterOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                  <el-select v-model="orderTimeFilter" clearable placeholder="时间筛选" class="filter-select">
                    <el-option label="全部时间" value="" />
                    <el-option
                      v-for="option in orderTimeFilterOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                </div>
              </div>
              <div v-if="ordersLoading">
                <el-skeleton :rows="4" animated />
              </div>
                <div v-else-if="filteredMyOrders.length > 0" class="table-card orders-table">
                <div class="table-head">
                  <span>预约房源</span>
                  <span>预约时间</span>
                  <span>创建时间</span>
                  <span>订单状态</span>
                  <span>支付状态</span>
                  <span class="action-head">操作</span>
                </div>
                <div
                  v-for="order in filteredMyOrders"
                  :key="order.id"
                  class="table-row"
                >
                  <!-- 预约房源优先展示真实房源标题，兼容不同接口结构 -->
                  <span class="title-cell">{{ getOrderHouseTitleWithFallback(order) }}</span>
                  <span>{{ formatDateTime(order.appointmentTime) }}</span>
                  <!-- 兼容后端实际字段 createTime 与历史字段 createdAt -->
                  <span>{{ formatDate(order.createTime || order.createdAt) }}</span>
                  <span>
                    <el-tag :type="orderStatusType(order.status)" size="small">
                      {{ orderStatusLabel(order.status) }}
                    </el-tag>
                  </span>
                  <span>
                    <el-tag :type="paymentStatusType(order.paymentStatus)" size="small">
                      {{ paymentStatusLabel(order.paymentStatus) }}
                    </el-tag>
                  </span>
                  <div class="row-actions">
                    <!-- 按需求明确动作语义：预约订单管理“查看”统一改为“查看订单” -->
                    <el-button size="small" @click="$router.push(`/orders/${order.id}`)">查看订单</el-button>
                    <!-- 按产品要求：租客端“退款”紧随“查看”并上下对齐，便于快速识别资金相关操作 -->
                    <el-button
                      v-if="canShowRefundAction(order)"
                      size="small"
                      type="info"
                      plain
                      @click="handleRefundOrder(order)"
                    >
                      退款
                    </el-button>
                    <el-button
                      size="small"
                      type="success"
                      v-if="canShowReviewAction(order)"
                      @click="openReviewDialog(order)"
                    >
                      去评价
                    </el-button>
                    <!-- 预约订单操作按条件直接显示，避免“更多操作”隐藏关键入口 -->
                    <el-button
                      v-if="order.contractId"
                      size="small"
                      type="primary"
                      plain
                      @click="router.push(`/contracts/${order.contractId}`)"
                    >
                      查看合同
                    </el-button>
                    <el-button
                      v-if="isTenant && order.status === 'PENDING'"
                      size="small"
                      type="danger"
                      plain
                      @click="cancelMyOrder(order.id)"
                    >
                      取消预约
                    </el-button>
                    <el-button
                      v-if="canShowPayAction(order)"
                      size="small"
                      type="warning"
                      plain
                      @click="handlePayOrder(order)"
                    >
                      待支付
                    </el-button>
                  </div>
                </div>
              </div>
              <el-empty v-else description="暂无预约记录" />
            </el-tab-pane>

            <!-- Contracts Tab -->
            <el-tab-pane v-if="!isAdmin" name="contracts">
              <template #label>
                <div class="tab-label">
                  <el-icon><Memo /></el-icon>
                  <span>合同管理</span>
                </div>
              </template>
              <div class="table-toolbar">
                <!-- 合同搜索栏：仅按文本字段搜索，合同状态改为右侧下拉筛选 -->
                <el-input
                  v-model.trim="contractSearchKeyword"
                  class="search-input"
                  clearable
                  placeholder="搜索合同（合同编号/租期/租金）"
                />
                <!-- 合同筛选区：状态维度单独下拉，避免与关键字搜索混用 -->
                <div class="toolbar-filters">
                  <el-select v-model="contractStatusFilter" clearable placeholder="合同状态" class="filter-select">
                    <el-option label="全部合同状态" value="" />
                    <el-option
                      v-for="option in contractStatusFilterOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                </div>
              </div>
              <div v-if="contractsLoading">
                <el-skeleton :rows="4" animated />
              </div>
              <div v-else-if="filteredMyContracts.length > 0" class="table-card contracts-table">
                <div class="table-head">
                  <span>合同编号</span>
                  <span>租期</span>
                  <span>月租</span>
                  <span>状态</span>
                  <span class="action-head">操作</span>
                </div>
                <div
                  v-for="contract in filteredMyContracts"
                  :key="contract.id"
                  class="table-row"
                >
                  <span class="title-cell">{{ contract.contractNo || contract.id }}</span>
                  <span>{{ formatDate(contract.startDate) }} 至 {{ formatDate(contract.endDate) }}</span>
                  <span>¥{{ contract.monthlyRent ?? contract.rent }}</span>
                  <span>
                    <el-tag :type="contractStatusType(contract.status)" size="small">
                      {{ contractStatusLabel(contract.status) }}
                    </el-tag>
                  </span>
                  <div class="row-actions">
                    <!-- 按需求明确动作语义：合同管理“查看”统一改为“查看合同” -->
                    <el-button size="small" @click="$router.push(`/contracts/${contract.id}`)">查看合同</el-button>
                    <el-button
                      v-if="contract.orderId"
                      size="small"
                      type="primary"
                      plain
                      @click="$router.push(`/orders/${contract.orderId}`)"
                    >
                      查看订单
                    </el-button>
                  </div>
                </div>
              </div>
              <el-empty v-else description="暂无合同记录" />
            </el-tab-pane>

            <!-- Review Management Tab -->
            <el-tab-pane v-if="!isAdmin" name="reviews">
              <template #label>
                <div class="tab-label">
                  <el-icon><Star /></el-icon>
                  <span>{{ isLandlord ? '收到的评价' : '评价管理' }}</span>
                </div>
              </template>
              <div v-if="reviewsLoading">
                <el-skeleton :rows="4" animated />
              </div>
                <div v-else-if="reviewRecords.length > 0" class="review-list">
                  <div v-for="review in reviewRecords" :key="review.id" class="review-item">
                    <div class="review-item-head">
                      <h4 class="review-house-title">{{ review.houseTitle || (review.houseId ? `房源#${review.houseId}` : '-') }}</h4>
                      <el-rate :model-value="review.rating || 0" disabled show-score />
                    </div>
                    <div class="review-item-meta">
                      <!-- 租客端与房东端统一展示双方身份信息，减少沟通歧义 -->
                      <span>房东：{{ review.landlordName || (review.landlordId ? `用户#${review.landlordId}` : '-') }}</span>
                      <span>租客：{{ review.tenantName || (review.tenantId ? `用户#${review.tenantId}` : '-') }}</span>
                      <!-- 评价订单标识优先展示业务 order_no，缺失时回退主键 ID 兼容历史数据 -->
                      <span>订单编号：{{ review.orderNo || review.orderId || '-' }}</span>
                    </div>
                    <!-- 将评价时间独立为单独一行并左对齐，避免在元信息拥挤时视觉跳动 -->
                    <div class="review-item-time">评价时间：{{ formatDateTime(review.createTime) }}</div>
                    <div class="review-item-content">{{ review.content || '（未填写评价内容）' }}</div>
                  </div>
                </div>
                <el-empty v-else :description="isLandlord ? '暂无收到的评价' : '暂无评价记录'" />
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
                        <span class="score-total">/200</span>
                      </div>
                      <el-progress
                      :percentage="creditProgress"
                      :color="creditColor(creditScore)"
                      :stroke-width="16"
                      class="credit-progress"
                    />
                    <p class="credit-label">{{ creditLabel(creditScore) }}</p>
                  </div>
                  <div class="credit-desc">
                    <h4>信用评分说明</h4>
                    <p>信用评分反映您在平台上的信誉状况，由交易记录、合同履约情况等综合计算；系统满分为200分，持续良好行为可逐步提升到100分以上。</p>
                    <ul>
                      <li>90-200分：优秀信用，享受优先推荐</li>
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

      <!-- 评价弹窗：租客对已完成订单进行一次性评价 -->
      <el-dialog v-model="reviewDialogVisible" title="订单评价" width="500px">
        <el-form label-width="90px">
          <el-form-item label="房源">
            <div class="review-house-title">{{ getOrderHouseTitleWithFallback(reviewTargetOrder) }}</div>
          </el-form-item>
          <el-form-item label="评分" required>
            <el-rate v-model="reviewForm.rating" :max="5" show-score />
          </el-form-item>
          <el-form-item label="评价内容">
            <el-input
              v-model="reviewForm.content"
              type="textarea"
              :rows="4"
              maxlength="500"
              show-word-limit
              placeholder="请填写您的看房/签约体验（选填）"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="reviewDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submittingReview" @click="submitOrderReview">
            提交评价
          </el-button>
        </template>
      </el-dialog>

      <Footer />
    </div>
  </template>

<script setup>
// 说明：个人中心页逻辑，管理用户资料编辑、密码修改、预约订单、合同、消息和信用评分展示
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'     // 用于密码修改后跳转到登录页与读取路由参数
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled, ChatLineSquare, Calendar, Memo, StarFilled, Star } from '@element-plus/icons-vue'
import NavBar from '../components/NavBar.vue'
import Footer from '../components/Footer.vue'
import MessageList from '../components/MessageList.vue'
import HouseCard from '../components/HouseCard.vue'
import { useUserStore } from '../stores/user.js'
import { changePassword as changePasswordApi, realNameAuth as realNameAuthApi } from '../api/auth.js'
import { getMyOrders, getLandlordOrders, cancelOrder, refundOrder, reviewOrder, getTenantReviewRecords, getLandlordReviewRecords } from '../api/order.js'
import { createAlipayPayForm } from '../api/alipay.js'
import { getMyContracts } from '../api/contract.js'
import { getMessages, markRead, markAllRead } from '../api/message.js'
import { getMyCollections, uploadHouseImage } from '../api/house.js'

const router = useRouter()                 // 获取路由实例以便在密码修改后跳转
const route = useRoute()                   // 获取当前路由，用于识别 ?tab=xxx 参数
const userStore = useUserStore()
const activeTab = ref('profile')      // 当前激活的 tab 标签名
const savingProfile = ref(false)      // 保存资料按钮 loading 状态
const uploadingAvatar = ref(false)    // 头像上传按钮 loading 状态
const changingPwd = ref(false)        // 修改密码按钮 loading 状态
const submittingRealName = ref(false) // 实名认证按钮 loading 状态
const ordersLoading = ref(false)      // 订单列表加载状态
const contractsLoading = ref(false)   // 合同列表加载状态
const collectionsLoading = ref(false) // 收藏列表加载状态
const reviewsLoading = ref(false)     // 评价列表加载状态
const myOrders = ref([])              // 当前用户的预约订单列表
const myContracts = ref([])           // 当前用户的合同列表
const myCollections = ref([])         // 收藏的房源列表
const collectionSearchKeyword = ref('') // 收藏搜索关键字（仅前端过滤，避免额外接口开销）
const orderSearchKeyword = ref('')      // 预约订单搜索关键字（仅前端过滤，避免额外接口开销）
const contractSearchKeyword = ref('')   // 合同搜索关键字（仅前端过滤，避免额外接口开销）
// 预约订单筛选状态：将状态、支付、时间从搜索框中拆分为独立维度
const orderStatusFilter = ref('')
const paymentStatusFilter = ref('')
const orderTimeFilter = ref('')
// 合同筛选状态：将合同状态从搜索框中拆分为独立维度
const contractStatusFilter = ref('')
const messages = ref([])              // 消息通知列表
// 评价管理列表数据：
// - 租客：展示“我提交的评价”
// - 房东：展示“我收到的评价”
const reviewRecords = ref([])
const reviewDialogVisible = ref(false) // 评价弹窗显隐
const submittingReview = ref(false)    // 提交评价按钮 loading
const reviewTargetOrder = ref(null)    // 当前正在评价的订单
const reviewForm = reactive({
  rating: 0,     // 星级评分（1~5）
  content: ''    // 评价文本（可选）
})
const profileFormRef = ref(null)
const pwdFormRef = ref(null)

// 从 Pinia store 计算用户信息
const userInfo = computed(() => userStore.userInfo)
const creditScore = computed(() => userInfo.value.creditScore || 100)
// 信用分满分调整为 200：进度条组件要求 0~100，因此这里换算为百分比展示。
// 同时用 Math.min 做上限保护，避免异常数据导致进度条超出。
const creditProgress = computed(() => Math.min(100, Math.round((creditScore.value / 200) * 100)))
const unreadMessages = computed(() => messages.value.filter(m => !m.isRead).length)
const isRealNameAuth = computed(() => !!userInfo.value.isRealNameAuth)
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
const isLandlord = computed(() => userInfo.value.role === 'LANDLORD')
const isAdmin = computed(() => userInfo.value.role === 'ADMIN')
/**
 * 允许通过 URL query 指定的标签页白名单，避免无效参数污染界面状态。
 * 例如：/user-center?tab=orders 会自动切换到“预约管理”。
 */
const allowedTabs = computed(() => {
  // 按角色返回可见标签集合，确保 URL query.tab 与页面可见模块严格一致：
  // - ADMIN：仅保留基础信息、消息和信用；
  // - LANDLORD：无“收藏”标签；
  // - TENANT：包含“收藏”。
  if (isAdmin.value) return ['profile', 'messages', 'credit']
  if (isLandlord.value) return ['profile', 'orders', 'contracts', 'messages', 'reviews', 'credit']
  if (isTenant.value) return ['profile', 'orders', 'favorites', 'contracts', 'messages', 'reviews', 'credit']
  return ['profile', 'messages', 'credit']
})

/**
 * 将当前激活标签同步到 URL query.tab。
 * 说明：顶部导航栏激活态基于 route.query.tab 计算，所以中心页内切换标签时也要同步 query。
 */
function syncTabToRouteQuery(tabName) {
  const targetTab = typeof tabName === 'string' ? tabName : ''
  if (!allowedTabs.value.includes(targetTab)) return
  if (route.query.tab === targetTab) return
  const nextQuery = { ...route.query, tab: targetTab }
  router.replace({ path: route.path, query: nextQuery })
}

/**
 * 统一搜索文本归一化函数：
 * 1) 将 null/undefined 安全转换为空串；
 * 2) 去除首尾空格；
 * 3) 小写化，保证匹配不区分大小写。
 */
function normalizeSearchText(value) {
  return String(value ?? '').trim().toLowerCase()
}

/**
 * 判断关键字是否命中候选字段：
 * - 关键字为空时，默认返回 true（展示全部数据）；
 * - 关键字非空时，只要任一候选字段包含关键字即命中。
 */
function containsKeyword(keyword, candidates) {
  const normalizedKeyword = normalizeSearchText(keyword)
  if (!normalizedKeyword) return true
  return candidates.some(item => normalizeSearchText(item).includes(normalizedKeyword))
}

/**
 * 通用下拉筛选匹配函数：
 * - 未选择筛选值时返回 true（不过滤）；
 * - 选择后要求字段值与筛选值严格相等。
 */
function matchesSelectFilter(actualValue, selectedValue) {
  if (!selectedValue) return true
  return actualValue === selectedValue
}

/**
 * 时间区间筛选：
 * - today：今日 00:00:00 ~ 次日 00:00:00；
 * - last7days：最近 7 天（含今天）；
 * - last30days：最近 30 天（含今天）。
 * 说明：优先使用 appointmentTime，缺失时回退 createTime/createdAt，兼容历史字段。
 */
function matchesOrderTimeFilter(order, selectedTimeRange) {
  if (!selectedTimeRange) return true
  const rawTime = order?.appointmentTime || order?.createTime || order?.createdAt
  if (!rawTime) return false

  const targetTime = new Date(rawTime).getTime()
  if (!Number.isFinite(targetTime)) return false

  const now = new Date()
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()

  if (selectedTimeRange === 'today') {
    const tomorrowStart = todayStart + 24 * 60 * 60 * 1000
    return targetTime >= todayStart && targetTime < tomorrowStart
  }
  if (selectedTimeRange === 'last7days') {
    // 近7天（含今天）：今天 + 前6天，共7个自然日（起点为6天前00:00，终点为当前时刻）。
    const rangeStart = todayStart - 6 * 24 * 60 * 60 * 1000
    return targetTime >= rangeStart
  }
  if (selectedTimeRange === 'last30days') {
    // 近30天（含今天）：今天 + 前29天，共30个自然日（起点为29天前00:00，终点为当前时刻）。
    const rangeStart = todayStart - 29 * 24 * 60 * 60 * 1000
    return targetTime >= rangeStart
  }
  return true
}

// 订单状态下拉选项: 复用状态枚举，保证展示文案与列表标签一致
const orderStatusFilterOptions = [
  { label: orderStatusLabel('PENDING'), value: 'PENDING' },
  { label: orderStatusLabel('APPROVED'), value: 'APPROVED' },
  { label: orderStatusLabel('REJECTED'), value: 'REJECTED' },
  { label: orderStatusLabel('CANCELLED'), value: 'CANCELLED' },
  { label: orderStatusLabel('COMPLETED'), value: 'COMPLETED' }
]

// 支付状态下拉选项: 保持与支付状态标签文案一致
const paymentStatusFilterOptions = [
  { label: paymentStatusLabel('UNPAID'), value: 'UNPAID' },
  { label: paymentStatusLabel('PAID'), value: 'PAID' },
  { label: paymentStatusLabel('REFUNDED'), value: 'REFUNDED' }
]

// 时间筛选下拉选项: 用于替代搜索框中的时间文本匹配
const orderTimeFilterOptions = [
  { label: '今日', value: 'today' },
  { label: '近7天', value: 'last7days' },
  { label: '近30天', value: 'last30days' }
]

// 合同状态下拉选项: 复用合同状态枚举文案
const contractStatusFilterOptions = [
  { label: contractStatusLabel('DRAFT'), value: 'DRAFT' },
  { label: contractStatusLabel('PENDING_SIGN'), value: 'PENDING_SIGN' },
  { label: contractStatusLabel('TENANT_SIGNED'), value: 'TENANT_SIGNED' },
  { label: contractStatusLabel('LANDLORD_SIGNED'), value: 'LANDLORD_SIGNED' },
  { label: contractStatusLabel('FULLY_SIGNED'), value: 'FULLY_SIGNED' },
  { label: contractStatusLabel('CANCELLED'), value: 'CANCELLED' }
]

/**
 * 收藏列表前端搜索结果：
 * 支持标题、城市、区县、地址、租金、面积字段，便于租客在收藏中快速定位房源。
 */
const filteredMyCollections = computed(() => myCollections.value.filter(house => containsKeyword(collectionSearchKeyword.value, [
  house.title,
  house.city,
  house.district,
  house.address,
  house.price,
  house.area
])))

/**
 * 预约订单前端搜索与筛选结果：
 * - 搜索框仅匹配订单号、房源名；
 * - 订单状态、支付状态、时间范围由独立下拉框筛选。
 * 说明：
 * - 订单号(orderNo)是用户在客服沟通、线下核对时最常使用的唯一标识；
 * - 将状态/时间从搜索框剥离后，用户可先按维度筛选，再用关键字精准定位。
 */
const filteredMyOrders = computed(() => myOrders.value.filter(order => (
  containsKeyword(orderSearchKeyword.value, [
    order.orderNo,
    getOrderHouseTitleWithFallback(order)
  ]) &&
  matchesSelectFilter(order.status, orderStatusFilter.value) &&
  matchesSelectFilter(order.paymentStatus, paymentStatusFilter.value) &&
  matchesOrderTimeFilter(order, orderTimeFilter.value)
)))

/**
 * 合同列表前端搜索与筛选结果：
 * - 搜索框仅匹配合同编号、租期、租金；
 * - 合同状态由独立下拉框筛选。
 */
const filteredMyContracts = computed(() => myContracts.value.filter(contract => (
  containsKeyword(contractSearchKeyword.value, [
    contract.contractNo,
    contract.id,
    formatDate(contract.startDate),
    formatDate(contract.endDate),
    contract.monthlyRent,
    contract.rent
  ]) &&
  matchesSelectFilter(contract.status, contractStatusFilter.value)
)))

// 资料编辑表单（初始值从 store 取）
const profileForm = reactive({
  username: userInfo.value.username,
  phone: userInfo.value.phone,
  email: userInfo.value.email,
  avatarUrl: userInfo.value.avatarUrl,
  gender: userInfo.value.gender ?? 0
})

// 中国大陆身份证号码基础格式：18 位，前 17 位数字，最后 1 位数字或 X
const CHINA_ID_CARD_REGEX = /^\d{17}[\dXx]$/
// 身份证校验位计算权重（国家标准 GB 11643）
const ID_CARD_WEIGHTS = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
// 身份证校验位映射表：sum % 11 后对应的最后一位字符
const ID_CARD_CHECK_CODES = ['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2']

// 实名认证表单
const realNameForm = reactive({
  realName: userInfo.value.realName || '',
  idCard: userInfo.value.idCard || ''
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
      avatarUrl: userInfo.value.avatarUrl,
      gender: userInfo.value.gender ?? 0
    })
    Object.assign(realNameForm, {
      realName: userInfo.value.realName || '',
      idCard: userInfo.value.idCard || ''
    })
  } catch (e) { /* ignore */ }

  // 并发加载订单、合同、消息数据
  loadOrders()
  loadContracts()
  loadMessages()
  loadCollections()
  loadReviewRecords()
})

/**
 * 监听路由 query.tab 变化：
 * - 支持在当前页面内重复点击顶栏导航时动态切换 tab
 * - 对无效 tab 值自动忽略，防止错误参数影响页面
 */
watch(
  () => route.query.tab,
  (tab) => {
    const targetTab = typeof tab === 'string' ? tab : ''
    if (!allowedTabs.value.includes(targetTab)) return
    // favorites 仅租客可见：非租客误传该 tab 时回退到 profile，防止进入不存在标签页
    if (targetTab === 'favorites' && !isTenant.value) {
      activeTab.value = 'profile'
      return
    }
    activeTab.value = targetTab
  },
  { immediate: true }
)

/**
 * 用户在页面内手动切换 tab 时，同步 query.tab，保证顶部导航高亮实时联动。
 */
watch(
  activeTab,
  (tab) => {
    syncTabToRouteQuery(tab)
  }
)

/**
 * 监听支付回跳标记：
 * 当从支付宝回跳页进入个人中心（fromPay=1）时，主动刷新订单列表，确保“支付状态”立即从未支付更新为已支付。
 */
watch(
  () => route.query.fromPay,
  async (flag) => {
    if (flag !== '1') return
    await loadOrders()
    // 刷新完成后移除一次性标记，避免后续普通切换 tab 时重复触发请求。
    const nextQuery = { ...route.query }
    delete nextQuery.fromPay
    router.replace({ path: route.path, query: nextQuery })
  },
  { immediate: true }
)

/** 加载当前用户的预约订单列表（租客取我的预约；房东取收到的预约） */
async function loadOrders() {
  // 管理员不参与预约流程，个人中心不展示订单列表，直接返回避免无效请求。
  if (isAdmin.value) {
    myOrders.value = []
    ordersLoading.value = false
    return
  }
  ordersLoading.value = true
  try {
    const res = isTenant.value
      ? await getMyOrders({ page: 1, size: 20 })
      : await getLandlordOrders({ page: 1, size: 50 })
    // 后端返回 PageResult 对象，其数据列表字段为 records（非 list）
    myOrders.value = Array.isArray(res) ? res : (res?.records || [])
  } catch (e) { /* ignore */ }
  finally { ordersLoading.value = false }
}

/**
 * 校验身份证中的出生日期是否为有效自然日（如 20240229 合法，20230229 非法）。
 * 说明：
 * 1) 先按 yyyy-MM-dd 构造 Date，若结果为 Invalid Date 直接判无效；
 * 2) 再反向比对年月日，防止 JS 自动进位把非法日期“纠正”为下个月日期。
 */
function isValidBirthDateInIdCard(birthText) {
  const year = Number(birthText.slice(0, 4))
  const month = Number(birthText.slice(4, 6))
  const day = Number(birthText.slice(6, 8))
  // 使用 new Date(year, monthIndex, day) 避免字符串日期受时区解析影响。
  const date = new Date(year, month - 1, day)
  return !Number.isNaN(date.getTime()) &&
    date.getFullYear() === year &&
    date.getMonth() + 1 === month &&
    date.getDate() === day
}

/**
 * 根据身份证前 17 位计算校验位（国家标准 GB 11643）。
 * @param {string} base17 - 身份证前 17 位数字字符串
 * @returns {string} 计算出的校验位字符（0-9 或 X）
 */
function calculateIdCardCheckCode(base17) {
  const sum = base17
    .split('')
    .reduce((acc, digit, index) => acc + Number(digit) * ID_CARD_WEIGHTS[index], 0)
  return ID_CARD_CHECK_CODES[sum % 11]
}

/**
 * 实名认证身份证号校验（格式 + 出生日期 + 校验位）。
 * 返回统一结构，便于在提交前给出明确、可读的错误提示。
 */
function validateChineseIdCard(idCard) {
  const normalized = normalizeIdCard(idCard)
  if (!CHINA_ID_CARD_REGEX.test(normalized)) {
    return { valid: false, message: '身份证号格式不正确，请输入18位身份证号' }
  }
  const birthText = normalized.slice(6, 14)
  if (!isValidBirthDateInIdCard(birthText)) {
    return { valid: false, message: '身份证号中的出生日期无效，请检查后重试' }
  }
  const expectedCode = calculateIdCardCheckCode(normalized.slice(0, 17))
  if (normalized[17] !== expectedCode) {
    return { valid: false, message: '身份证号校验位不正确，请检查后重试' }
  }
  return { valid: true, normalized }
}

/** 统一身份证号归一化：去除首尾空格并将校验位字符转为大写。 */
function normalizeIdCard(idCard) {
  return String(idCard ?? '').trim().toUpperCase()
}

/** 统一真实姓名归一化：去除首尾空格，避免保存无意义空白字符。 */
function normalizeRealName(realName) {
  return String(realName ?? '').trim()
}

/** 提交实名认证 */
async function submitRealNameAuth() {
  if (isRealNameAuth.value) return
  const realName = normalizeRealName(realNameForm.realName)
  const idCard = normalizeIdCard(realNameForm.idCard)
  if (!realName || !idCard) {
    ElMessage.warning('请输入真实姓名和身份证号')
    return
  }
  // 提交前做前端预校验，及时反馈错误，减少无效请求打到后端。
  const idCardValidation = validateChineseIdCard(idCard)
  if (!idCardValidation.valid) {
    ElMessage.warning(idCardValidation.message)
    return
  }
  submittingRealName.value = true
  try {
    await realNameAuthApi({
      realName,
      // 统一转大写，保证 X/x 表示的一致性，便于后端落库与后续比对。
      idCard: idCardValidation.normalized
    })
    await userStore.fetchProfile()
    Object.assign(realNameForm, {
      realName: userInfo.value.realName || '',
      idCard: userInfo.value.idCard || ''
    })
    ElMessage.success('实名认证提交成功')
  } catch (e) {
    ElMessage.error(e.message || '实名认证失败')
  } finally {
    submittingRealName.value = false
  }
}

/** 加载当前用户参与的合同列表（最多20条） */
async function loadContracts() {
  // 管理员在个人中心不展示合同管理，清空数据并跳过接口调用。
  if (isAdmin.value) {
    myContracts.value = []
    contractsLoading.value = false
    return
  }
  contractsLoading.value = true
  try {
    const res = await getMyContracts({ page: 1, size: 20 })
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
    const res = await getMyCollections({ page: 1, size: 30 })
    myCollections.value = Array.isArray(res)
      ? res
      : (res?.records || res?.list || [])
  } catch (e) { /* ignore */ }
  finally { collectionsLoading.value = false }
}

/** 加载当前用户的消息通知列表（最多50条） */
async function loadMessages() {
  try {
    const res = await getMessages({ page: 1, size: 50 })
    // 后端返回 PageResult 对象，其数据列表字段为 records（非 list）
    messages.value = Array.isArray(res) ? res : (res?.records || [])
  } catch (e) { /* ignore */ }
}

/**
 * 加载评价管理数据（按角色分流）：
 * 1) TENANT：调用“我提交的评价”接口；
 * 2) LANDLORD：调用“我收到的评价”接口；
 * 3) 其他角色（如 ADMIN）：不发起请求，直接置空列表。
 *
 * 这样可避免房东在个人中心进入“评价管理”时仍走租客接口，
 * 导致页面错误显示“暂无评价记录”。
 */
async function loadReviewRecords() {
  // 管理员不参与评价体系，避免请求租客/房东评价接口。
  if (isAdmin.value) {
    reviewRecords.value = []
    reviewsLoading.value = false
    return
  }
  if (!isTenant.value && !isLandlord.value) {
    reviewRecords.value = []
    reviewsLoading.value = false
    return
  }
  reviewsLoading.value = true
  try {
    const res = isLandlord.value
      ? await getLandlordReviewRecords({ page: 1, size: 50 })
      : await getTenantReviewRecords({ page: 1, size: 50 })
    reviewRecords.value = Array.isArray(res) ? res : (res?.records || [])
  } catch (e) {
    // 失败时清空列表，避免保留旧数据误导用户
    reviewRecords.value = []
  }
  finally { reviewsLoading.value = false }
}

/** 保存用户资料修改 */
async function saveProfile() {
  savingProfile.value = true
  try {
    // updateProfile 内部已做 avatar/avatarUrl 兼容并回写 store，可直接驱动头像回显。
    await userStore.updateProfile(profileForm)
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    savingProfile.value = false
  }
}

/**
 * 本地上传头像（与发布房源图片上传逻辑保持一致）：
 * 1) 仅允许图片类型文件；
 * 2) 上传成功后将返回 URL 回填到资料表单；
 * 3) 不自动保存，仍需用户点击“保存修改”统一提交资料变更。
 */
async function handleAvatarFileChange(uploadFile) {
  const rawFile = uploadFile?.raw
  if (!rawFile) {
    ElMessage.warning('未获取到上传文件，请重试')
    return
  }
  if (!rawFile.type?.startsWith('image/')) {
    ElMessage.warning('仅支持上传图片文件')
    return
  }
  uploadingAvatar.value = true
  try {
    const avatarUrl = await uploadHouseImage(rawFile)
    profileForm.avatarUrl = avatarUrl || ''
    ElMessage.success('头像上传成功，请点击“保存修改”生效')
  } catch (e) {
    ElMessage.error(e.message || '头像上传失败，请重试')
  } finally {
    uploadingAvatar.value = false
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
 * 租客支付订单（支付宝沙箱，同步回调）：
 * 1) 调后端生成支付宝官方表单；
 * 2) 将表单写入新窗口并自动提交到支付宝收银台；
 * 3) 用户支付后回跳到前端 return 页面，再由该页面调用后端验签落库。
 */
async function handlePayOrder(order) {
  try {
    await ElMessageBox.confirm(
      '即将进入支付界面，确认支付后订单将变为“已完成”。',
      '支付确认',
      { type: 'warning', confirmButtonText: '确认支付', cancelButtonText: '取消' }
    )
    const res = await createAlipayPayForm(order.id)
    const formHtml = res?.formHtml
    if (!formHtml) {
      throw new Error(`支付表单生成失败（订单ID：${order.id}）`)
    }

    // 通过新窗口承载支付宝表单，避免当前个人中心页被覆盖，提升支付后返回体验。
    const payWindow = window.open('', '_blank')
    if (!payWindow) {
      throw new Error('浏览器拦截了弹窗，请在浏览器设置中允许本站弹窗后重试')
    }
    payWindow.document.open()
    payWindow.document.write(formHtml)
    payWindow.document.close()

    ElMessage.success('已打开支付宝支付页面，请在新窗口完成支付')
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e.message || '支付失败')
  }
}

/**
 * 租客退款订单：
 * 仅在订单已支付时显示“退款”按钮，退款成功后订单状态为已取消、支付状态为已退款。
 */
async function handleRefundOrder(order) {
  try {
    await ElMessageBox.confirm(
      '确认发起退款？退款后订单状态将变为“已取消”。',
      '退款确认',
      { type: 'warning', confirmButtonText: '确认退款', cancelButtonText: '取消' }
    )
    await refundOrder(order.id)
    ElMessage.success('退款成功')
    loadOrders()
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e.message || '退款失败')
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
  const map = { PENDING: '待房东确认', APPROVED: '房东已确认', REJECTED: '房东已拒绝', CANCELLED: '订单已取消', COMPLETED: '订单已完成' }
  return map[status] || status
}

/** 订单状态对应的 Element Plus Tag 类型 */
function orderStatusType(status) {
  const map = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info', COMPLETED: 'primary' }
  return map[status] || 'info'
}

/** 支付状态枚举转中文 */
function paymentStatusLabel(status) {
  const map = { UNPAID: '未支付', PAID: '已支付', REFUNDED: '已退款' }
  return map[status] || status || '-'
}

/** 支付状态对应的 Element Plus Tag 类型 */
function paymentStatusType(status) {
  const map = { UNPAID: 'warning', PAID: 'success', REFUNDED: 'info' }
  return map[status] || 'info'
}

/**
 * 是否展示“待支付”按钮：
 * - 仅租客可见；
 * - 订单必须已批准；
 * - 支付状态为未支付；
 * - 合同状态已达到双方签署完成（后端通过 canPay 返回）。
 */
function canShowPayAction(order) {
  return isTenant.value &&
    order?.status === 'APPROVED' &&
    order?.paymentStatus === 'UNPAID' &&
    order?.canPay === true
}

/** 是否展示“退款”按钮：仅租客且已支付订单可见 */
function canShowRefundAction(order) {
  return isTenant.value && order?.paymentStatus === 'PAID'
}

/**
 * 是否展示“去评价”按钮：
 * - 仅租客可见；
 * - 订单状态必须是 COMPLETED；
 * - reviewed 为 false（或未返回）时展示，避免重复评价。
 */
function canShowReviewAction(order) {
  return isTenant.value &&
    order?.status === 'COMPLETED' &&
    order?.reviewed !== true
}

/** 打开评价弹窗并初始化表单 */
function openReviewDialog(order) {
  reviewTargetOrder.value = order
  reviewForm.rating = 0
  reviewForm.content = ''
  reviewDialogVisible.value = true
}

/** 提交订单评价：评分必填，成功后关闭弹窗并刷新订单列表。 */
async function submitOrderReview() {
  if (!reviewTargetOrder.value?.id) return
  // el-rate 默认值为 0（未选择），有效评分范围为 1~5，这里只需判断是否已选择。
  if (!reviewForm.rating) {
    ElMessage.warning('请先选择1-5星评分')
    return
  }
  submittingReview.value = true
  try {
    await reviewOrder(reviewTargetOrder.value.id, {
      rating: reviewForm.rating,
      content: reviewForm.content.trim() || null
    })
    ElMessage.success('评价提交成功')
    reviewDialogVisible.value = false
    await loadOrders()
    await loadReviewRecords()
  } catch (e) {
    ElMessage.error(e.message || '评价提交失败')
  } finally {
    submittingReview.value = false
  }
}

/** 合同状态枚举转中文 */
function contractStatusLabel(status) {
  const map = { DRAFT: '草稿', PENDING_SIGN: '待签署', TENANT_SIGNED: '租客已签', LANDLORD_SIGNED: '房东已签', FULLY_SIGNED: '双方已签', CANCELLED: '已取消' }
  return map[status] || status
}

/** 合同状态对应的 Element Plus Tag 类型 */
function contractStatusType(status) {
  const map = { DRAFT: 'info', PENDING_SIGN: 'warning', TENANT_SIGNED: 'warning', LANDLORD_SIGNED: 'warning', FULLY_SIGNED: 'success', CANCELLED: 'danger' }
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

function formatDateTime(date) {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN', { hour12: false })
}

/**
 * 获取订单对应房源标题（用于“预约管理”列表展示）
 * 兼容后端可能返回的多种字段结构，保证尽量展示可读标题而不是纯 ID。
 */
function getOrderHouseTitleWithFallback(order) {
  return order?.house?.title || order?.houseTitle || (order?.houseId ? `房源#${order.houseId}` : '-')
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
  --orders-table-cols: 2fr 1.2fr 1.2fr 1fr 1fr 1.8fr;   /* title | appointment | created | orderStatus | payStatus | actions */
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

.table-toolbar {
  margin: 16px 0 12px;
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
}

.search-input {
  width: 320px;
  max-width: 100%;
}

/* 搜索栏右侧筛选区：承载状态/时间下拉，支持自动换行适配小屏 */
.toolbar-filters {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.filter-select {
  width: 140px;
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

.avatar-upload-field {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
  width: 100%;
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
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}

/* 仅预约订单操作区使用纵向左对齐布局，避免影响合同等其他 tab 的操作区展示 */
.orders-table .row-actions {
  display: flex;
  flex-wrap: wrap;
  row-gap: 8px;
  column-gap: 8px;
  align-items: center;
  justify-content: flex-start;
}

/* 统一按钮宽度与外边距，避免不同操作组合下出现“挤压/错行/不齐”的观感 */
.orders-table .row-actions :deep(.el-button) {
  min-width: 88px;
  margin-left: 0;
}

/* “操作”列表头与按钮区域保持视觉居中，便于快速定位操作入口 */
.action-head {
  text-align: center;
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

.review-list {
  display: grid;
  gap: 12px;
  padding: 12px 0;
}

.review-item {
  border: 1px solid #edf0f7;
  border-radius: 10px;
  padding: 12px 14px;
  background: #fff;
}

.review-item-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.review-item-meta {
  margin-top: 8px;
  color: #8a94a6;
  font-size: 12px;
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-start;
}

/* 将评价时间独立为左对齐信息行，避免与身份/订单信息挤在同一行导致错位 */
.review-item-time {
  margin-top: 8px;
  color: #8a94a6;
  font-size: 12px;
  text-align: left;
}

.review-item-content {
  margin-top: 8px;
  font-size: 13px;
  color: #2b3445;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 900px) {
  .user-center-page {
    --profile-avatar-form-cols: 1fr;
    --user-center-stat-min: 150px;
  }

  /* 小屏下下拉筛选区左对齐并占满一行，避免与搜索框重叠 */
  .toolbar-filters {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
