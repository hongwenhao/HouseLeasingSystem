import request from './index'
export const createContract = (data) => request.post('/contracts', data)
export const getContractDetail = (id) => request.get(`/contracts/${id}`)
export const getMyContracts = (params) => request.get('/contracts/my', { params })
export const signContract = (id) => request.put(`/contracts/${id}/sign`)
export const terminateContract = (id, data) => request.put(`/contracts/${id}/terminate`, data)
export const getContractRisks = (id) => request.get(`/contracts/${id}/risks`)
