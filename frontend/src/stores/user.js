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

/**
 * 统一用户对象字段：
 * - 后端字段为 avatar；
 * - 前端历史代码广泛使用 avatarUrl。
 * 这里做双向兼容，避免头像“保存成功但页面不回显”。
 */
function normalizeUserInfo(user = {}) {
  const avatarUrl = user.avatarUrl || user.avatar || ''
  return {
    ...user,
    avatar: user.avatar || avatarUrl,
    avatarUrl
  }
}

export const useUserStore = defineStore('user', {
  /** 初始化状态：优先从 localStorage 恢复 token 和 role（刷新后保持登录态） */
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: {
      id: null,
      username: '',
      phone: '',
      email: '',
      role: localStorage.getItem('role') || '', // 角色：TENANT / LANDLORD / ADMIN
      avatar: '',
      avatarUrl: '',
      creditScore: 100  // 默认信用分 100
    },
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
      this.userInfo = { id: null, username: '', phone: '', email: '', role: '', avatar: '', avatarUrl: '', creditScore: 100 }
      this.isLoggedIn = false
      // 清除本地存储中的认证信息
      localStorage.removeItem('token')
      localStorage.removeItem('role')
    },

    /**
     * 获取当前登录用户的最新个人信息
     * 在进入个人中心页面时调用，确保展示最新数据
     */
    async fetchProfile() {
      const res = await getProfile()
      this.userInfo = normalizeUserInfo(res)
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
      if (Object.prototype.hasOwnProperty.call(payload, 'avatarUrl')) {
        payload.avatar = payload.avatarUrl
        delete payload.avatarUrl
      }
      const res = await updateProfileApi(payload)
      this.userInfo = normalizeUserInfo({ ...this.userInfo, ...res })
      return this.userInfo
    }
  }
})
