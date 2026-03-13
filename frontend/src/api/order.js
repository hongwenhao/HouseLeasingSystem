/**
 * api/order.js —— 预约/订单相关接口
 *
 * 封装租房预约（订单）的全生命周期接口：
 *   - POST /orders              创建预约订单（租客发起预约看房）
 *   - GET  /orders/:id          获取单个订单详情
 *   - GET  /orders/my/tenant    获取当前租客的所有订单
 *   - GET  /orders/my/landlord  获取当前房东收到的所有预约订单
 *   - PUT  /orders/:id/confirm  房东确认预约
 *   - PUT  /orders/:id/reject   房东拒绝预约（携带拒绝原因）
 *   - PUT  /orders/:id/cancel   租客取消预约
 *   - PUT  /orders/:id/complete 将订单标记为完成状态
 */

import request from './index'

/** 创建预约订单，data 包含 houseId / appointmentDate / message 等 */
export const createOrder = (data) => request.post('/orders', data)

/** 获取指定订单详情（含关联的房源和用户信息） */
export const getOrderDetail = (id) => request.get(`/orders/${id}`)

/** 获取租客自己的预约订单列表 */
export const getMyOrders = (params) => request.get('/orders/my/tenant', { params })

/** 获取房东收到的预约订单列表 */
export const getLandlordOrders = (params) => request.get('/orders/my/landlord', { params })

/** 房东确认预约，订单状态变为 CONFIRMED */
export const confirmOrder = (id) => request.put(`/orders/${id}/confirm`)

/** 房东拒绝预约，data 包含 { reason: '拒绝原因' } */
export const rejectOrder = (id, data) => request.put(`/orders/${id}/reject`, data)

/** 租客取消预约，订单状态变为 CANCELLED */
export const cancelOrder = (id) => request.put(`/orders/${id}/cancel`)

/** 将已确认的订单标记为完成（实际看房/成交后） */
export const completeOrder = (id) => request.put(`/orders/${id}/complete`)
