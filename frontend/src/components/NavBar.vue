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
        <!-- 已登录用户可见：预约订单管理，按角色跳转到对应页面并定位到 orders 标签页 -->
        <router-link
          v-if="isLoggedIn"
          :to="ordersNavTarget"
          active-class=""
          exact-active-class=""
          :class="['nav-link', { 'tab-active': isOrdersActive }]"
          @click="menuOpen = false"
        >预约订单管理</router-link>
        <!-- 已登录用户可见：合同管理，按角色跳转到对应页面并定位到 contracts 标签页 -->
        <router-link
          v-if="isLoggedIn"
          :to="contractsNavTarget"
          active-class=""
          exact-active-class=""
          :class="['nav-link', { 'tab-active': isContractsActive }]"
          @click="menuOpen = false"
        >合同管理</router-link>
        <!-- 已登录用户可见：消息中心（目前统一在个人中心 messages 标签页展示） -->
        <router-link
          v-if="isLoggedIn"
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
        <!-- 仅管理员可见：管理后台入口 -->
        <router-link
          v-if="role === 'ADMIN'"
          to="/admin"
          class="nav-link"
          @click="menuOpen = false"
        >管理后台</router-link>
      </div>

      <!-- 右侧操作区域 -->
      <div class="nav-right">
        <!-- 消息角标：已登录时显示，展示未读消息数量 -->
        <el-badge :value="unreadCount > 0 ? unreadCount : ''" class="msg-badge" v-if="isLoggedIn">
          <router-link to="/user-center" class="icon-btn">
            <el-icon size="20"><Bell /></el-icon>
          </router-link>
        </el-badge>

        <!-- 已登录：显示用户头像和下拉菜单 -->
        <template v-if="isLoggedIn">
          <el-dropdown @command="handleCommand">
            <span class="user-avatar-wrap">
              <el-avatar
                :size="32"
                :src="userInfo.avatarUrl || ''"
                :icon="UserFilled"
                class="user-avatar"
              />
              <span class="username">{{ userInfo.username }}</span>
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
// 读取用户角色，优先从 store，降级读取 localStorage（页面刷新时）
const role = computed(() => userStore.userInfo.role || localStorage.getItem('role') || '')
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
// 顶栏“消息中心”目标路由：消息目前统一在个人中心 messages 标签页
const messagesNavTarget = computed(() => ({ path: '/user-center', query: { tab: 'messages' } }))

const isCenterRoute = computed(() => route.path === '/user-center' || route.path === '/landlord-center')
const isOrdersActive = computed(() => isCenterRoute.value && route.query.tab === 'orders')
const isContractsActive = computed(() => isCenterRoute.value && route.query.tab === 'contracts')
const isMessagesActive = computed(() => isCenterRoute.value && route.query.tab === 'messages')

onMounted(async () => {
  // 已登录时，异步拉取未读消息数量，展示在消息角标上
  if (isLoggedIn.value) {
    try {
      const res = await getUnreadCount()
      // 兼容后端直接返回数字或 { count: n } 两种格式
      unreadCount.value = typeof res === 'number' ? res : (res?.count ?? 0)
    } catch (e) {
      // 获取未读数失败时不影响页面正常使用，静默忽略
    }
  }
})

/**
 * 处理用户下拉菜单命令
 * @param {string} cmd - 'profile'（跳转个人中心）或 'logout'（退出登录）
 */
async function handleCommand(cmd) {
  if (cmd === 'profile') {
    router.push('/user-center')
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
}

.username {
  font-size: 14px;
  color: #303133;
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
