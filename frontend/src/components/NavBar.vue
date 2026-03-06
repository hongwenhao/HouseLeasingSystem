<template>
  <nav class="navbar">
    <div class="navbar-inner">
      <router-link to="/" class="navbar-brand">
        <img src="../assets/logo.svg" alt="logo" class="logo-icon" />
        <span class="brand-text">房屋租赁系统</span>
      </router-link>

      <div class="nav-links" :class="{ open: menuOpen }">
        <router-link to="/" class="nav-link" @click="menuOpen = false">首页</router-link>
        <router-link to="/houses" class="nav-link" @click="menuOpen = false">房源列表</router-link>
        <router-link
          v-if="role === 'LANDLORD'"
          to="/landlord-center"
          class="nav-link"
          @click="menuOpen = false"
        >房东中心</router-link>
        <router-link
          v-if="role === 'LANDLORD'"
          to="/publish-house"
          class="nav-link"
          @click="menuOpen = false"
        >发布房源</router-link>
        <router-link
          v-if="role === 'ADMIN'"
          to="/admin"
          class="nav-link"
          @click="menuOpen = false"
        >管理后台</router-link>
      </div>

      <div class="nav-right">
        <el-badge :value="unreadCount > 0 ? unreadCount : ''" class="msg-badge" v-if="isLoggedIn">
          <router-link to="/user-center" class="icon-btn">
            <el-icon size="20"><Bell /></el-icon>
          </router-link>
        </el-badge>

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
        <template v-else>
          <router-link to="/login" class="auth-btn">登录</router-link>
          <router-link to="/register" class="auth-btn primary">注册</router-link>
        </template>

        <button class="hamburger" @click="menuOpen = !menuOpen">
          <el-icon><Menu /></el-icon>
        </button>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user.js'
import { getUnreadCount } from '../api/message.js'
import { UserFilled } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const menuOpen = ref(false)
const unreadCount = ref(0)

const isLoggedIn = computed(() => userStore.isLoggedIn)
const userInfo = computed(() => userStore.userInfo)
const role = computed(() => userStore.userInfo.role || localStorage.getItem('role') || '')

onMounted(async () => {
  if (isLoggedIn.value) {
    try {
      const res = await getUnreadCount()
      unreadCount.value = typeof res === 'number' ? res : (res?.count ?? 0)
    } catch (e) {
      // ignore
    }
  }
})

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
.navbar {
  position: sticky;
  top: 0;
  z-index: 1000;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.navbar-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 24px;
}

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

.brand-text {
  font-size: 18px;
  font-weight: 700;
  color: #409eff;
}

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

.nav-link:hover,
.nav-link.router-link-active {
  color: #409eff;
  background: #ecf5ff;
}

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

.auth-btn {
  padding: 6px 16px;
  border-radius: 6px;
  text-decoration: none;
  font-size: 14px;
  color: #606266;
  border: 1px solid #dcdfe6;
  transition: all 0.2s;
}

.auth-btn:hover {
  color: #409eff;
  border-color: #409eff;
}

.auth-btn.primary {
  background: #409eff;
  color: #fff;
  border-color: #409eff;
}

.auth-btn.primary:hover {
  background: #66b1ff;
  border-color: #66b1ff;
}

.hamburger {
  display: none;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 20px;
  color: #606266;
}

@media (max-width: 768px) {
  .hamburger {
    display: flex;
    align-items: center;
  }

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
    z-index: 999;
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
