import request from './index'
export const createOrder = (data) => request.post('/orders', data)
export const getOrderDetail = (id) => request.get(`/orders/${id}`)
export const getMyOrders = (params) => request.get('/orders/my', { params })
export const getLandlordOrders = (params) => request.get('/orders/landlord', { params })
export const confirmOrder = (id) => request.put(`/orders/${id}/confirm`)
export const rejectOrder = (id, data) => request.put(`/orders/${id}/reject`, data)
export const cancelOrder = (id) => request.put(`/orders/${id}/cancel`)
export const completeOrder = (id) => request.put(`/orders/${id}/complete`)
