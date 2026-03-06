import request from './index'
export const getMessages = (params) => request.get('/messages', { params })
export const markRead = (id) => request.put(`/messages/${id}/read`)
export const markAllRead = () => request.put('/messages/read-all')
export const getUnreadCount = () => request.get('/messages/unread-count')
