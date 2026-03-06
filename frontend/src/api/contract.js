/**
 * api/contract.js —— 租房合同相关接口
 *
 * 封装合同的创建、查询、签署、终止及风险检测接口：
 *   - POST /contracts              根据已确认的订单生成合同（房东操作）
 *   - GET  /contracts/:id          获取合同详情（含条款和双方信息）
 *   - GET  /contracts/my           获取当前用户（租客/房东）参与的合同列表
 *   - PUT  /contracts/:id/sign     当前用户签署合同（双方分别调用）
 *   - PUT  /contracts/:id/terminate 终止已生效合同（携带终止原因）
 *   - GET  /contracts/:id/risks    AI 智能检测合同风险条款，返回风险列表
 */

import request from './index'

/** 创建合同，data 包含 orderId 等关联信息 */
export const createContract = (data) => request.post('/contracts', data)

/** 获取合同详情，含双方信息、签署状态、合同条款 */
export const getContractDetail = (id) => request.get(`/contracts/${id}`)

/** 获取当前用户参与的合同列表（租客或房东均可调用） */
export const getMyContracts = (params) => request.get('/contracts/my', { params })

/** 当前用户签署合同，双方均需签署后合同才正式生效 */
export const signContract = (id) => request.put(`/contracts/${id}/sign`)

/** 终止合同，data 包含 { reason: '终止原因' } */
export const terminateContract = (id, data) => request.put(`/contracts/${id}/terminate`, data)

/** 获取合同风险检测结果，返回风险条款列表（含风险等级 HIGH/MEDIUM/LOW） */
export const getContractRisks = (id) => request.get(`/contracts/${id}/risks`)
