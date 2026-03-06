import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, getProfile, updateProfile as updateProfileApi } from '../api/auth.js'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: {
      id: null,
      username: '',
      phone: '',
      email: '',
      role: localStorage.getItem('role') || '',
      avatarUrl: '',
      creditScore: 100
    },
    isLoggedIn: !!localStorage.getItem('token')
  }),
  actions: {
    async login(credentials) {
      const res = await loginApi(credentials)
      this.token = res.token
      this.userInfo = res.user
      localStorage.setItem('token', res.token)
      localStorage.setItem('role', res.user.role)
      this.isLoggedIn = true
    },
    async logout() {
      try {
        await logoutApi()
      } catch (e) {
        // ignore
      }
      this.token = ''
      this.userInfo = { id: null, username: '', phone: '', email: '', role: '', avatarUrl: '', creditScore: 100 }
      this.isLoggedIn = false
      localStorage.removeItem('token')
      localStorage.removeItem('role')
    },
    async fetchProfile() {
      const res = await getProfile()
      this.userInfo = res
      return res
    },
    async updateProfile(data) {
      const res = await updateProfileApi(data)
      this.userInfo = { ...this.userInfo, ...res }
      return res
    }
  }
})
