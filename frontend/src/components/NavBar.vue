<template>
  <!-- 组件说明：顶部导航栏组件，固定定位在页面顶部，包含品牌 Logo、主导航链接、
       用户头像下拉菜单（已登录）或登录/注册按钮（未登录），以及消息未读数角标。
       支持移动端响应式：小屏下导航链接收起，通过汉堡按钮展开。 -->
  <nav class="navbar">
    <div class="navbar-inner">
      <!-- 品牌 Logo 区域，点击跳转首页 -->
      <router-link to="/" class="navbar-brand">
        <img src="../assets/logo.svg" alt="logo" class="logo-icon" />
        <span class="brand-text">房屋租赁系统</span>
      </router-link>

      <!-- 主导航链接区域（移动端通过 :class="{ open: menuOpen }" 控制显隐） -->
      <div class="nav-links" :class="{ open: menuOpen }">
        <router-link to="/" class="nav-link" @click="menuOpen = false">首页</router-link>
        <router-link to="/houses" class="nav-link" @click="menuOpen = false">房源列表</router-link>
        <!-- 仅管理员可见：管理后台分模块入口，与 /admin?tab=xxx 联动 -->
        <router-link
          v-if="isAdmin"
          :to="adminUsersNavTarget"
          active-class=""
          exact-active-class=""
          :class="['nav-link', { 'tab-active': isAdminUsersActive }]"
          @click="menuOpen = false"
        >用户管理</router-link>
        <router-link
          v-if="isAdmin"
          :to="adminHouseMgmtNavTarget"
          active-class=""
          exact-active-class=""
          :class="['nav-link', { 'tab-active': isAdminHouseMgmtActive }]"
          @click="menuOpen = false"
        >房源管理</router-link>
        <router-link
          v-if="isAdmin"
          :to="adminOrdersNavTarget"
          active-class=""
          exact-active-class=""
          :class="['nav-link', { 'tab-active': isAdminOrdersActive }]"
          @click="menuOpen = false"
        >订单管理</router-link>
        <router-link
          v-if="isAdmin"
          :to="adminContractsNavTarget"
          active-class=""
          exact-active-class=""
          :class="['nav-link', { 'tab-active': isAdminContractsActive }]"
          @click="menuOpen = false"
        >合同管理</router-link>
        <router-link
          v-if="isAdmin"
          :to="adminOverviewNavTarget"
          active-class=""
          exact-active-class=""
          :class="['nav-link', { 'tab-active': isAdminOverviewActive }]"
          @click="menuOpen = false"
        >数据概览</router-link>
        <!-- 仅租客可见：我的收藏入口，按要求放在“房源列表”右侧 -->
        <router-link
          v-if="isLoggedIn && role === 'TENANT'"
          :to="favoritesNavTarget"
          active-class=""
          exact-active-class=""
          :class="['nav-link', { 'tab-active': isFavoritesActive }]"
          @click="menuOpen = false"
        >我的收藏</router-link>
        <!-- 已登录用户可见：预约订单管理，按角色跳转到对应页面并定位到 orders 标签页 -->
        <router-link
          v-if="isLoggedIn && !isAdmin"
          :to="ordersNavTarget"
          active-class=""
          exact-active-class=""
          :class="['nav-link', { 'tab-active': isOrdersActive }]"
          @click="menuOpen = false"
        >预约订单管理</router-link>
        <!-- 已登录用户可见：合同管理，按角色跳转到对应页面并定位到 contracts 标签页 -->
        <router-link
          v-if="isLoggedIn && !isAdmin"
          :to="contractsNavTarget"
          active-class=""
          exact-active-class=""
          :class="['nav-link', { 'tab-active': isContractsActive }]"
          @click="menuOpen = false"
        >合同管理</router-link>
        <!-- 已登录用户可见：评价入口，租客显示“评价管理”，房东显示“收到的评价”，并放在合同管理后面 -->
        <router-link
          v-if="isLoggedIn && !isAdmin"
          :to="reviewsNavTarget"
          active-class=""
          exact-active-class=""
          :class="['nav-link', { 'tab-active': isReviewsActive }]"
          @click="menuOpen = false"
        >{{ role === 'LANDLORD' ? '收到的评价' : '评价管理' }}</router-link>
        <!-- 已登录用户可见：消息中心（目前统一在个人中心 messages 标签页展示） -->
        <router-link
          v-if="isLoggedIn && !isAdmin"
          :to="messagesNavTarget"
          active-class=""
          exact-active-class=""
          :class="['nav-link', { 'tab-active': isMessagesActive }]"
          @click="menuOpen = false"
        >消息中心</router-link>
        <!-- 仅房东角色可见：房东中心入口 -->
        <router-link
          v-if="role === 'LANDLORD'"
          to="/landlord-center"
          class="nav-link"
          @click="menuOpen = false"
        >房东中心</router-link>
        <!-- 仅房东角色可见：发布房源入口 -->
        <router-link
          v-if="role === 'LANDLORD'"
          to="/publish-house"
          class="nav-link"
          @click="menuOpen = false"
        >发布房源</router-link>
      </div>

      <!-- 右侧操作区域 -->
      <div class="nav-right">
        <!-- 消息角标：已登录时显示，展示未读消息数量 -->
        <el-badge :value="unreadCount > 0 ? unreadCount : ''" class="msg-badge" v-if="isLoggedIn">
          <!-- 铃铛入口与“消息中心”导航保持同一目标，点击直接进入消息标签页 -->
          <router-link :to="messagesNavTarget" class="icon-btn">
            <el-icon size="20"><Bell /></el-icon>
          </router-link>
        </el-badge>

        <!-- 已登录：显示用户头像和下拉菜单 -->
        <template v-if="isLoggedIn">
          <el-dropdown @command="handleCommand">
            <span class="user-avatar-wrap">
              <el-avatar
                :size="32"
                :src="displayAvatar"
                :icon="UserFilled"
                class="user-avatar"
              />
              <span class="username" :title="displayUsername">{{ displayUsername }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon> 个人中心
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <!-- 未登录：显示登录和注册按钮 -->
        <template v-else>
          <router-link to="/login" class="auth-btn">登录</router-link>
          <router-link to="/register" class="auth-btn primary">注册</router-link>
        </template>

        <!-- 移动端汉堡菜单按钮（小屏下展示，点击切换导航展开/收起） -->
        <button class="hamburger" @click="menuOpen = !menuOpen">
          <el-icon><Menu /></el-icon>
        </button>
      </div>
    </div>
  </nav>
</template>

<script setup>
// 说明：顶部导航栏逻辑，负责获取登录状态、未读消息数，以及处理用户下拉菜单命令
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user.js'
import { getUnreadCount } from '../api/message.js'
import { USER_INFO_STORAGE_KEY } from '../constants/storageKeys.js'
import { UserFilled } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
// 控制移动端汉堡菜单的展开/收起状态
const menuOpen = ref(false)
// 未读消息数量，用于消息角标
const unreadCount = ref(0)

// 从 Pinia store 读取登录状态和用户信息
const isLoggedIn = computed(() => userStore.isLoggedIn)
const userInfo = computed(() => userStore.userInfo)
// 导航头像显示值：优先取 avatar，再回退 avatarUrl，避免字段名不一致导致头像丢失
const displayAvatar = computed(() => userInfo.value.avatar || userInfo.value.avatarUrl || '')
// 导航用户名显示值：刷新时先读 store，缺失时回退本地缓存，避免短暂空白
const displayUsername = computed(() => {
  const fromStore = (userInfo.value.username || '').trim()
  if (fromStore) return fromStore
  try {
    const cached = JSON.parse(localStorage.getItem(USER_INFO_STORAGE_KEY) || '{}')
    return (cached?.username || '').trim() || '用户'
  } catch (e) {
    return '用户'
  }
})
// 读取用户角色，优先从 store，降级读取 localStorage（页面刷新时）
const role = computed(() => userStore.userInfo.role || localStorage.getItem('role') || '')
// 角色快捷判断：便于模板按角色精确控制导航项显示
const isAdmin = computed(() => role.value === 'ADMIN')
// 顶栏“预约订单管理”目标路由：房东进入房东中心订单标签，其他角色进入个人中心订单标签
const ordersNavTarget = computed(() => (
  role.value === 'LANDLORD'
    ? { path: '/landlord-center', query: { tab: 'orders' } }
    : { path: '/user-center', query: { tab: 'orders' } }
))
// 顶栏“合同管理”目标路由：房东进入房东中心合同标签，其他角色进入个人中心合同标签
const contractsNavTarget = computed(() => (
  role.value === 'LANDLORD'
    ? { path: '/landlord-center', query: { tab: 'contracts' } }
    : { path: '/user-center', query: { tab: 'contracts' } }
))
// 顶栏“我的收藏”目标路由：仅租客进入个人中心 favorites 标签
const favoritesNavTarget = computed(() => ({ path: '/user-center', query: { tab: 'favorites' } }))
// 顶栏“评价管理/收到的评价”目标路由：房东进入房东中心 reviews 标签，其他角色进入个人中心 reviews 标签
const reviewsNavTarget = computed(() => (
  role.value === 'LANDLORD'
    ? { path: '/landlord-center', query: { tab: 'reviews' } }
    : { path: '/user-center', query: { tab: 'reviews' } }
))
// 顶栏“消息中心”目标路由：消息目前统一在个人中心 messages 标签页
const messagesNavTarget = computed(() => ({ path: '/user-center', query: { tab: 'messages' } }))
// 顶栏“管理员分模块”目标路由：统一落到 /admin 并携带 query.tab，与后台标签联动
const adminOverviewNavTarget = computed(() => ({ path: '/admin', query: { tab: 'overview' } }))
const adminUsersNavTarget = computed(() => ({ path: '/admin', query: { tab: 'users' } }))
const adminHouseMgmtNavTarget = computed(() => ({ path: '/admin', query: { tab: 'houseMgmt' } }))
const adminOrdersNavTarget = computed(() => ({ path: '/admin', query: { tab: 'orders' } }))
const adminContractsNavTarget = computed(() => ({ path: '/admin', query: { tab: 'contracts' } }))

const isCenterRoute = computed(() => route.path === '/user-center' || route.path === '/landlord-center')
const isAdminRoute = computed(() => route.path === '/admin')
const isOrdersActive = computed(() => isCenterRoute.value && route.query.tab === 'orders')
const isContractsActive = computed(() => isCenterRoute.value && route.query.tab === 'contracts')
// 仅在个人中心 favorites 标签时点亮“我的收藏”导航
const isFavoritesActive = computed(() => route.path === '/user-center' && route.query.tab === 'favorites')
const isReviewsActive = computed(() => isCenterRoute.value && route.query.tab === 'reviews')
const isMessagesActive = computed(() => isCenterRoute.value && route.query.tab === 'messages')
// 管理员后台导航激活态：支持无 tab 参数时默认点亮“数据概览”
const isAdminOverviewActive = computed(() => isAdminRoute.value && (!route.query.tab || route.query.tab === 'overview'))
const isAdminUsersActive = computed(() => isAdminRoute.value && route.query.tab === 'users')
const isAdminHouseMgmtActive = computed(() => isAdminRoute.value && route.query.tab === 'houseMgmt')
const isAdminOrdersActive = computed(() => isAdminRoute.value && route.query.tab === 'orders')
const isAdminContractsActive = computed(() => isAdminRoute.value && route.query.tab === 'contracts')

onMounted(async () => {
  // 已登录时并行做两件事：
  // 1) 刷新未读消息角标；2) 兜底拉取用户资料，修复刷新后头像/用户名偶发不显示。
  if (!isLoggedIn.value) return
  await Promise.allSettled([loadUnreadCount(), ensureNavProfileReady()])
})

/**
 * 拉取未读消息数：
 * - 兼容后端返回 number 与 { count } 两种结构；
 * - 失败静默降级，不阻塞导航栏其它功能。
 */
async function loadUnreadCount() {
  try {
    const res = await getUnreadCount()
    unreadCount.value = typeof res === 'number' ? res : (res?.count ?? 0)
  } catch (e) {
    // 获取未读数失败时不影响页面正常使用，静默忽略
  }
}

/**
 * 确保导航栏基础资料（用户名/角色）可用：
 * - 刷新后如果 store 中缺关键字段，主动补拉 profile；
 * - 失败时保持现有缓存展示，避免用户看到“空头像 + 空用户名”。
 */
async function ensureNavProfileReady() {
  const hasUsername = !!(userInfo.value.username || '').trim()
  const hasRole = !!(userInfo.value.role || localStorage.getItem('role') || '')
  if (hasUsername && hasRole) return
  try {
    await userStore.fetchProfile()
  } catch (e) {
    // 静默降级：此处不打断导航栏渲染，保留本地缓存显示
  }
}

/**
 * 处理用户下拉菜单命令。
 * @param {string} cmd - 'profile'（进入个人信息）或 'logout'（退出登录）
 */
async function handleCommand(cmd) {
  if (cmd === 'profile') {
    // 下拉“个人中心”统一按“进入个人信息”处理：
    // 1) 管理员：仍按原产品设计进入后台用户管理（/admin?tab=users）。
    //    原因：管理员“个人中心”入口在本系统语义上更偏向“后台工作台入口”，
    //    其高频任务是用户治理与平台运营，不使用租客/房东的个人资料页流程。
    // 2) 房东/租客：都跳转到个人中心的“个人信息”标签页（/user-center?tab=profile）。
    //    这样可确保两端点击“个人中心”后都直接落到“个人信息”界面，避免房东误入业务管理标签页。
    if (role.value === 'ADMIN') {
      router.push({ path: '/admin', query: { tab: 'users' } })
    } else {
      router.push({ path: '/user-center', query: { tab: 'profile' } })
    }
  } else if (cmd === 'logout') {
    await userStore.logout()
    router.push('/')
  }
}
</script>

<style scoped>
/* ===== 导航栏容器：毛玻璃效果 + 底部细线分隔 ===== */
.navbar {
  position: sticky;      /* 粘性定位，滚动时保持在顶部 */
  top: 0;
  z-index: 1000;         /* 确保覆盖页面其他元素 */
  background: rgba(255, 255, 255, 0.85);   /* 半透明白色背景 */
  backdrop-filter: blur(20px);              /* 毛玻璃模糊效果 */
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);  /* 细微底部分隔线 */
  box-shadow: 0 1px 12px rgba(0, 0, 0, 0.04);
}

.navbar-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 64px;
  display: flex;
  align-items: center;
  gap: 24px;
}

/* ===== 品牌 Logo 区域 ===== */
.navbar-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  flex-shrink: 0;
}

.logo-icon {
  width: 32px;
  height: 32px;
}

/* 品牌名称渐变色文字 */
.brand-text {
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, #409eff, #1a6ebd);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* ===== 导航链接区域 ===== */
.nav-links {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.nav-link {
  padding: 6px 14px;
  text-decoration: none;
  color: #606266;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.2s;
}

/* 链接悬停：微妙的底部指示线；当前激活状态：蓝色文字 + 浅蓝背景 */
.nav-link:hover {
  color: #409eff;
  background: rgba(64, 158, 255, 0.06);
}

.nav-link.router-link-active {
  color: #409eff;
  background: #ecf5ff;
  font-weight: 500;
}

.nav-link.tab-active {
  color: #409eff;
  background: #ecf5ff;
  font-weight: 500;
}

/* ===== 右侧用户操作区域 ===== */
.nav-right {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
  flex-shrink: 0; /* 防止导航项过多时右侧头像与用户名被压缩到不可见 */
}

.msg-badge {
  cursor: pointer;
}

.icon-btn {
  display: flex;
  align-items: center;
  color: #606266;
  text-decoration: none;
}

.icon-btn:hover {
  color: #409eff;
}

.user-avatar-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  max-width: 180px;
}

.username {
  display: inline-block;
  font-size: 14px;
  color: #303133;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 登录/注册按钮样式（现代圆角胶囊风格） */
.auth-btn {
  padding: 7px 20px;
  border-radius: 20px;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  border: 1px solid #dcdfe6;
  transition: all 0.3s ease;
}

.auth-btn:hover {
  color: #409eff;
  border-color: #409eff;
  transform: translateY(-1px);
}

/* 注册按钮：渐变主色填充样式 */
.auth-btn.primary {
  background: linear-gradient(135deg, #409eff, #1a6ebd);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

.auth-btn.primary:hover {
  background: linear-gradient(135deg, #66b1ff, #409eff);
  border-color: transparent;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
  transform: translateY(-1px);
}

/* ===== 移动端汉堡菜单按钮（默认隐藏） ===== */
.hamburger {
  display: none;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 20px;
  color: #606266;
}

/* ===== 响应式：小屏幕适配 ===== */
@media (max-width: 768px) {
  .hamburger {
    display: flex;
    align-items: center;
  }

  /* 移动端导航链接：默认隐藏，展开时以下拉列表形式显示 */
  .nav-links {
    display: none;
    position: absolute;
    top: 60px;
    left: 0;
    right: 0;
    background: #fff;
    flex-direction: column;
    padding: 12px 0;
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
    z-index: 1001;
  }

  .nav-links.open {
    display: flex;
  }

  .nav-link {
    width: 100%;
    padding: 10px 20px;
    border-radius: 0;
  }
}
</style>
