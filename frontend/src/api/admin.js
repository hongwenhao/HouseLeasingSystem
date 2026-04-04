/**
 * api/admin.js —— 管理后台相关接口
 *
 * 封装管理员专用接口，仅 ADMIN 角色可访问：
 *   - GET /admin/stats               获取平台整体数据概览（用户数、房源数、合同数等）
 *   - GET /admin/users               分页获取用户列表（支持搜索）
 *   - GET /admin/houses              获取房源管理列表（支持关键词）
 *   - GET /admin/houses/:id          获取房源管理详情
 *   - PUT /admin/houses/:id/online   管理员上架房源
 *   - PUT /admin/houses/:id/offline  管理员下架房源
 *   - GET /admin/houses/pending      获取待审核的房源列表（兼容接口）
 *   - PUT /admin/houses/:id/audit    审核房源（兼容接口）
 *   - GET /admin/stats/area          获取各城市房源数量统计（用于柱状图）
 *   - GET /admin/stats/price-trends  获取近 N 个月租金均价趋势（用于折线图）
 *   - GET /admin/stats/credit        获取用户信用分分布统计（用于饼图）
 *   - PUT /admin/users/:id/ban       封禁指定用户账号
 *   - PUT /admin/users/:id/unban     解封指定用户账号
 */

import request from './index'

/** 获取平台数据概览：总用户数、总房源数、待审核数、成交合同数 */
export const getStats = () => request.get('/admin/stats')

/** 获取用户列表，params 支持 page/pageSize 及关键词搜索 */
export const getUserList = (params) => request.get('/admin/users', { params })

/** 获取管理员订单列表（分页） */
export const getOrderList = (params) => request.get('/admin/orders', { params })

/** 获取管理员合同列表（分页） */
export const getContractList = (params) => request.get('/admin/contracts', { params })

/** 获取管理员房源管理列表（分页） */
export const getHouseManagementList = (params) => request.get('/admin/houses', { params })

/** 获取管理员房源管理详情 */
export const getHouseManagementDetail = (id) => request.get(`/admin/houses/${id}`)

/** 管理员上架房源 */
export const putHouseOnlineByAdmin = (id) => request.put(`/admin/houses/${id}/online`)

/** 管理员下架房源 */
export const putHouseOfflineByAdmin = (id) => request.put(`/admin/houses/${id}/offline`)

/** 获取状态为 PENDING 的待审核房源列表 */
export const getPendingHouses = (params) => request.get('/admin/houses/pending', { params })

/** 审核房源，data 包含 { status: 'APPROVED' | 'REJECTED', reason? } */
export const auditHouseAdmin = (id, data) => request.put(`/admin/houses/${id}/audit`, data)

/** 获取各城市房源数量统计，用于渲染柱状图 */
export const getAreaStats = () => request.get('/admin/stats/area')

/** 获取近6个月租金均价趋势，用于渲染折线图 */
export const getPriceTrends = () => request.get('/admin/stats/price-trends')

/** 获取用户信用分区间分布，用于渲染饼图 */
export const getCreditDistribution = () => request.get('/admin/stats/credit')

/** 封禁用户，被封禁用户无法登录和使用平台功能 */
export const banUser = (id) => request.put(`/admin/users/${id}/ban`)

/** 解封用户，恢复用户的正常使用权限 */
export const unbanUser = (id) => request.put(`/admin/users/${id}/unban`)
