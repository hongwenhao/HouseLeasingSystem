/**
 * stores/user.js —— 用户状态管理 Store（Pinia）
 *
 * 管理当前登录用户的全局状态，包括：
 *   - token（JWT 令牌）
 *   - userInfo（用户基本信息：id、用户名、手机、角色、头像、信用分等）
 *   - isLoggedIn（是否已登录）
 *
 * 提供的 actions：
 *   - login()      发起登录请求，成功后缓存 token 和 role 到 localStorage
 *   - logout()     清除登录状态（同时调用后端登出接口）
 *   - fetchProfile() 从后端拉取最新用户信息
 *   - updateProfile() 更新用户资料并同步到本地 store
 */

import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, getProfile, updateProfile as updateProfileApi } from '../api/auth.js'

// 本地缓存 key：用于刷新后快速恢复用户名/头像，减少导航栏闪烁与空白
const USER_INFO_STORAGE_KEY = 'userInfo'

/**
 * 统一用户对象字段：
 * - 后端字段为 avatar；
 * - 前端历史代码广泛使用 avatarUrl。
 * 这里做双向兼容，避免头像“保存成功但页面不回显”。
 */
function normalizeUserInfo(user = {}) {
  // 明确优先级：以后端字段 avatar 为准，缺失时回退 avatarUrl。
  const avatar = user.avatar || user.avatarUrl || ''
  return {
    ...user,
    avatar,
    avatarUrl: avatar
  }
}

/**
 * 统一默认用户信息结构，避免在不同 action 中重复手写对象造成字段漂移。
 */
function getDefaultUserInfo() {
  return {
    id: null,
    username: '',
    phone: '',
    email: '',
    role: localStorage.getItem('role') || '', // 角色：TENANT / LANDLORD / ADMIN
    avatar: '',
    avatarUrl: '',
    creditScore: 100
  }
}

/**
 * 尝试从 localStorage 恢复用户信息：
 * - 成功：用于页面刷新后的首屏展示（头像/用户名）；
 * - 失败：回退默认对象，不阻断应用启动流程。
 */
function getCachedUserInfo() {
  try {
    const raw = localStorage.getItem(USER_INFO_STORAGE_KEY)
    if (!raw) return null
    return normalizeUserInfo(JSON.parse(raw) || {})
  } catch (e) {
    return null
  }
}

const initialUserInfo = getCachedUserInfo() || getDefaultUserInfo()

export const useUserStore = defineStore('user', {
  /** 初始化状态：优先从 localStorage 恢复 token 和 role（刷新后保持登录态） */
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: initialUserInfo,
    isLoggedIn: !!localStorage.getItem('token') // 根据 token 是否存在判断登录态
  }),
  actions: {
    /**
     * 用户登录
     * @param {Object} credentials - 登录凭据 { username, password }
     * 登录成功后将 token 和 role 持久化到 localStorage
     */
    async login(credentials) {
      const res = await loginApi(credentials)
      this.token = res.token
      this.userInfo = normalizeUserInfo(res.user)
      // 持久化存储 token 和角色，供路由守卫和 axios 拦截器读取
      localStorage.setItem('token', res.token)
      localStorage.setItem('role', res.user.role)
      localStorage.setItem(USER_INFO_STORAGE_KEY, JSON.stringify(this.userInfo))
      this.isLoggedIn = true
    },

    /**
     * 用户登出
     * 即使后端登出接口失败也继续清除本地登录状态，保证前端侧安全退出
     */
    async logout() {
      try {
        await logoutApi()
      } catch (e) {
        // 忽略后端登出失败，继续清除本地状态
      }
      this.token = ''
      this.userInfo = { ...getDefaultUserInfo(), role: '' }
      this.isLoggedIn = false
      // 清除本地存储中的认证信息
      localStorage.removeItem('token')
      localStorage.removeItem('role')
      localStorage.removeItem(USER_INFO_STORAGE_KEY)
    },

    /**
     * 获取当前登录用户的最新个人信息
     * 在进入个人中心页面时调用，确保展示最新数据
     */
    async fetchProfile() {
      const res = await getProfile()
      this.userInfo = normalizeUserInfo(res)
      localStorage.setItem(USER_INFO_STORAGE_KEY, JSON.stringify(this.userInfo))
      return this.userInfo
    },

    /**
     * 更新用户资料
     * @param {Object} data - 需要更新的字段
     * 仅更新变更字段，合并到现有 userInfo 中
     */
    async updateProfile(data) {
      // 后端 DTO 接收 avatar 字段，前端表单使用 avatarUrl 字段；这里统一转换后再提交。
      const payload = { ...data }
      if ('avatarUrl' in payload) {
        payload.avatar = payload.avatarUrl
        delete payload.avatarUrl
      }
      const res = await updateProfileApi(payload)
      this.userInfo = normalizeUserInfo({ ...this.userInfo, ...res })
      localStorage.setItem(USER_INFO_STORAGE_KEY, JSON.stringify(this.userInfo))
      return this.userInfo
    }
  }
})
