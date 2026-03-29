/**
 * api/contract.js —— 租房合同相关接口
 *
 * 与后端 ContractController 对齐的路由与方法：
 *   - POST /contracts/generate         根据已确认的订单生成合同（房东操作）
 *   - POST /contracts/:id/sign         当前用户签署合同（需传递 role: TENANT/LANDLORD）
 *   - PUT  /contracts/:id/cancel       取消/终止合同（草稿或待签署阶段）
 *   - GET  /contracts                  获取当前用户合同列表（参数：page/size/role）
 *   - GET  /contracts/:id              获取合同详情（含条款和双方信息）
 *   - POST /contracts/:id/risk-check   AI 智能检测合同风险条款，返回风险列表
 */

import request from './index'

/** 创建合同，data 包含 orderId 等关联信息 */
export const createContract = (data) => request.post('/contracts/generate', data)

/** 获取合同详情，含双方信息、签署状态、合同条款 */
export const getContractDetail = (id) => request.get(`/contracts/${id}`)

/** 获取当前用户参与的合同列表（租客或房东均可调用） */
export const getMyContracts = (params = {}) => {
  // 后端分页参数为 page/size，role 可选（TENANT/LANDLORD）
  const { page = 1, pageSize, size, role, ...rest } = params
  return request.get('/contracts', {
    params: { page, size: pageSize ?? size ?? 10, role, ...rest }
  })
}

/** 当前用户签署合同，双方均需签署后合同才正式生效 */
export const signContract = (id, role) => request.post(`/contracts/${id}/sign`, { role })

/** 终止合同，data 包含 { reason: '终止原因' } */
export const cancelContract = (id) => request.put(`/contracts/${id}/cancel`)

/** 获取合同风险检测结果，返回风险条款列表（含风险等级 HIGH/MEDIUM/LOW） */
export const getContractRisks = (id) => request.post(`/contracts/${id}/risk-check`)
