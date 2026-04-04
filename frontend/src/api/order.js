/**
 * api/order.js —— 预约/订单相关接口
 *
 * 封装租房预约（订单）的全生命周期接口：
 *   - POST /orders/appointment    创建预约订单（租客发起预约看房）
 *   - GET  /orders/:id            获取单个订单详情
 *   - GET  /orders/my/tenant      获取当前租客的所有订单
 *   - GET  /orders/my/landlord    获取当前房东收到的所有预约订单
 *   - PUT  /orders/:id/approve    房东审批预约（approve=true 确认，approve=false 拒绝）
 *   - PUT  /orders/:id/cancel     租客取消预约
 *   - PUT  /orders/:id/complete   将订单标记为完成状态
 *   - PUT  /orders/:id/pay        租客支付订单（合同双方已签后）
 *   - PUT  /orders/:id/refund     租客退款订单（已支付后）
 *   - POST /orders/:id/review     租客对已完成订单提交评价
 */

import request from './index'

/**
 * 创建预约看房订单
 * data 字段说明：
 *   houseId         {Long}          目标房源 ID（必填）
 *   appointmentTime {LocalDateTime} 预约看房时间（必填）
 *   remark          {String}        留言/备注（可选）
 */
export const createOrder = (data) => request.post('/orders/appointment', data)

/** 获取指定订单详情（含关联的房源和用户信息） */
export const getOrderDetail = (id) => request.get(`/orders/${id}`)

/** 获取租客自己的预约订单列表，params 支持 page/pageSize 分页参数 */
export const getMyOrders = (params) => request.get('/orders/my/tenant', { params })

/** 获取房东收到的预约订单列表，params 支持 page/pageSize 分页参数 */
export const getLandlordOrders = (params) => request.get('/orders/my/landlord', { params })

/**
 * 房东确认预约，发送 approved=true，订单状态变为 APPROVED
 * @param {number} id - 订单 ID
 */
export const confirmOrder = (id) => request.put(`/orders/${id}/approve`, { approved: true })

/**
 * 房东拒绝预约，发送 approved=false，订单状态变为 REJECTED
 * @param {number} id   - 订单 ID
 * @param {object} data - 附加信息，目前后端未记录拒绝原因，字段可预留
 */
export const rejectOrder = (id, data) => request.put(`/orders/${id}/approve`, { approved: false, ...data })

/** 租客取消预约，订单状态变为 CANCELLED */
export const cancelOrder = (id) => request.put(`/orders/${id}/cancel`)

/** 将已确认的订单标记为完成（实际看房/成交后） */
export const completeOrder = (id) => request.put(`/orders/${id}/complete`)

/** 租客支付订单：成功后后端会把订单改为 COMPLETED，支付状态改为 PAID */
export const payOrder = (id) => request.put(`/orders/${id}/pay`)

/** 租客退款订单：成功后后端会把订单改为 CANCELLED，支付状态改为 REFUNDED */
export const refundOrder = (id) => request.put(`/orders/${id}/refund`)

/**
 * 租客评价订单：
 * - 仅允许对“已完成(COMPLETED)”且未评价过的订单提交；
 * - data: { rating: 1~5, content?: string }。
 */
export const reviewOrder = (id, data) => request.post(`/orders/${id}/review`, data)
