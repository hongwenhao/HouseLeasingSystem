/**
 * api/index.js —— Axios HTTP 请求封装
 *
 * 创建并导出一个配置好的 axios 实例 `request`，统一处理：
 *   1. 请求拦截器：自动在请求头中附加 JWT Token（Bearer 认证）
 *   2. 响应拦截器：
 *      - 统一解包后端 { code, data, message } 格式的响应体
 *      - 非 200 业务码时抛出带错误信息的 Promise rejection
 *      - HTTP 401（未认证/Token 过期）时清除本地存储并跳转到登录页
 */

import axios from 'axios'

// 创建 axios 实例，统一设置基础 URL 和超时时间
const request = axios.create({
  baseURL: '/api',    // 对应 vite.config.js 中代理到后端 8080 端口的路径前缀
  timeout: 10000      // 请求超时时间：10 秒
})

// ===== 请求拦截器 =====
request.interceptors.request.use(
  (config) => {
    // 从本地存储读取 JWT Token，若存在则加入 Authorization 请求头
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ===== 响应拦截器 =====
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端统一响应格式：{ code: 200, data: ..., message: '...' }
    if (res.code !== undefined && res.code !== 200) {
      // 业务异常：非 200 状态码，抛出后端返回的错误信息
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    // 若存在 data 字段则直接返回 data，否则返回整个响应体（兼容无包装格式）
    return res.data !== undefined ? res.data : res
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      // HTTP 401：Token 不存在或已过期，清除认证信息并强制跳转登录页
      localStorage.removeItem('token')
      localStorage.removeItem('role')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default request
