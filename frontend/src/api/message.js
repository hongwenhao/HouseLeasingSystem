/**
 * api/message.js —— 站内消息/通知相关接口
 *
 * 封装用户消息通知的查询操作：
 *   - GET /messages                  分页获取当前用户的消息列表
 */

import request from './index'

/** 获取消息列表，params 支持分页（page / pageSize）和类型筛选 */
export const getMessages = (params) => request.get('/messages', { params })
