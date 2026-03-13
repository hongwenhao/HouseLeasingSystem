/**
 * api/index.js —— Axios HTTP 请求封装
 *
 * 创建并导出一个配置好的 axios 实例 `request`，统一处理：
 *   1. 请求拦截器：自动在请求头中附加 JWT Token（Bearer 认证）
 *   2. 响应拦截器：
 *      - 统一解包后端 { code, data, message } 格式的响应体
 *      - 非 200 业务码时抛出带中文错误信息的 Promise rejection
 *      - HTTP 401（未认证/Token 过期）时清除本地存储并跳转到登录页
 *      - 网络异常和超时统一转换为中文提示
 */

import axios from 'axios'

// 创建 axios 实例，统一设置基础 URL 和超时时间
const request = axios.create({
  baseURL: '/api',    // 对应 vite.config.js 中代理到后端 8080 端口的路径前缀
  timeout: 10000      // 请求超时时间：10 秒
})

/**
 * 将后端返回的英文错误消息映射为中文提示
 * 用于统一错误信息语言，提升用户体验
 * @param {string} msg - 后端返回的原始错误消息
 * @returns {string} 对应的中文错误提示
 */
function translateErrorMessage(msg) {
  if (!msg || typeof msg !== 'string') return '请求失败，请稍后重试'

  // 常见后端英文错误消息 → 中文映射表
  const errorMap = {
    'user not found': '用户不存在',
    'invalid credentials': '用户名或密码错误',
    'invalid password': '密码不正确',
    'wrong password': '密码不正确',
    'username already exists': '用户名已被注册',
    'phone already exists': '手机号已被注册',
    'email already exists': '邮箱已被注册',
    'unauthorized': '请先登录',
    'forbidden': '没有操作权限',
    'access denied': '没有操作权限',
    'token expired': '登录已过期，请重新登录',
    'invalid token': '登录信息无效，请重新登录',
    'house not found': '房源不存在',
    'order not found': '订单不存在',
    'contract not found': '合同不存在',
    'already exists': '数据已存在',
    'bad request': '请求参数错误',
    'internal server error': '服务器内部错误，请稍后重试',
    'service unavailable': '服务暂时不可用，请稍后重试',
    'network error': '网络连接失败，请检查网络',
    'timeout': '请求超时，请稍后重试',
    'validation failed': '数据验证失败，请检查输入',
    'not found': '请求的资源不存在',
    'duplicate entry': '数据已存在，请勿重复操作',
    'operation failed': '操作失败，请稍后重试',
    'user is banned': '该账户已被封禁',
    'account locked': '账户已被锁定',
    'password too short': '密码长度不足',
    'invalid phone number': '手机号格式不正确',
    'invalid email': '邮箱格式不正确',
  }

  // 将消息转小写后在映射表中查找匹配项（优先匹配更长的关键词，避免短关键词误匹配）
  const lowerMsg = msg.toLowerCase().trim()
  const sortedEntries = Object.entries(errorMap).sort((a, b) => b[0].length - a[0].length)
  for (const [key, value] of sortedEntries) {
    if (lowerMsg.includes(key)) {
      return value
    }
  }

  // 若消息已经是中文（包含中文字符），则直接返回
  if (/[\u4e00-\u9fa5]/.test(msg)) {
    return msg
  }

  // 其他未匹配的英文消息统一返回通用中文提示
  return '操作失败，请稍后重试'
}

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
      // 业务异常：非 200 状态码，将错误信息翻译为中文后抛出
      return Promise.reject(new Error(translateErrorMessage(res.message)))
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
    // 网络异常和超时错误转换为中文提示
    if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      return Promise.reject(new Error('请求超时，请检查网络后重试'))
    }
    if (error.message === 'Network Error') {
      return Promise.reject(new Error('网络连接失败，请检查网络设置'))
    }
    // 其他 HTTP 错误也翻译为中文
    const serverMsg = error.response?.data?.message
    if (serverMsg) {
      return Promise.reject(new Error(translateErrorMessage(serverMsg)))
    }
    return Promise.reject(new Error(translateErrorMessage(error.message)))
  }
)

export default request
