/**
 * api/message.js —— 站内消息/通知相关接口
 *
 * 封装用户消息通知的查询和已读标记操作：
 *   - GET /messages                  分页获取当前用户的消息列表
 *   - PUT /messages/:id/read         将单条消息标记为已读
 *   - PUT /messages/read-all         将当前用户所有未读消息批量标记为已读
 *   - GET /messages/unread-count     获取未读消息数量（用于导航栏小红点显示）
 */

import request from './index'

/** 获取消息列表，params 支持分页（page / pageSize）和类型筛选 */
export const getMessages = (params) => request.get('/messages', { params })

/** 将指定消息标记为已读 */
export const markRead = (id) => request.put(`/messages/${id}/read`)

/** 一键将所有未读消息标记为已读 */
export const markAllRead = () => request.put('/messages/read-all')

/** 获取未读消息总数，用于在导航栏展示消息角标 */
export const getUnreadCount = () => request.get('/messages/unread-count')
