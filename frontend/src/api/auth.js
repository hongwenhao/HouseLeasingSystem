/**
 * api/auth.js —— 用户认证相关接口
 *
 * 封装登录、注册、登出及用户资料相关的 HTTP 请求：
 *   - POST /auth/login    用户名/手机号 + 密码登录，返回 JWT token 和用户信息
 *   - POST /auth/register 新用户注册（租客或房东）
 *   - POST /auth/logout   服务端登出（使 token 失效）
 *   - GET  /user/profile  获取当前登录用户的详细信息
 *   - PUT  /user/profile  更新用户资料（用户名、手机、邮箱、头像等）
 *   - PUT  /user/password 修改登录密码（需提供旧密码验证）
 */

import request from './index'

/** 用户登录，返回 { token, user } */
export const login = (data) => request.post('/auth/login', data)

/** 用户注册，data 包含 username / phone / email / password / role */
export const register = (data) => request.post('/auth/register', data)

/** 服务端登出，使当前 JWT 令牌失效 */
export const logout = () => request.post('/auth/logout')

/** 获取当前登录用户的个人信息 */
export const getProfile = () => request.get('/user/profile')

/** 更新当前用户的个人资料 */
export const updateProfile = (data) => request.put('/user/profile', data)

/** 修改密码，data 包含 { oldPassword, newPassword } */
export const changePassword = (data) => request.put('/user/password', data)
