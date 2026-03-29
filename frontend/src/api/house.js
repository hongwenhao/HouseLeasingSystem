/**
 * api/house.js —— 房源相关接口
 *
 * 封装房源的 CRUD 及搜索、推荐、审核等接口：
 *   - GET    /houses               分页获取房源列表（支持多条件筛选）
 *   - GET    /houses/:id           获取单个房源完整详情
 *   - POST   /houses               发布新房源（仅房东可操作）
 *   - PUT    /houses/:id           更新房源信息（仅房源所有者）
 *   - DELETE /houses/:id           删除房源（仅房源所有者）
 *   - GET    /houses/my            获取当前房东发布的所有房源
 *   - GET    /houses/recommended   获取首页精选推荐房源
 *   - GET    /houses/search        按关键词搜索房源
 *   - PUT    /admin/houses/:id/audit 管理员审核房源（通过/拒绝）
 */

import request from './index'

/** 分页获取房源列表，params 支持 city/district/minPrice/maxPrice/rooms/ownerType 等筛选条件 */
export const getHouses = (params) => request.get('/houses', { params })

/** 获取指定房源的完整详情，含图片、费用配置、房东信息 */
export const getHouseDetail = (id) => request.get(`/houses/${id}`)

/** 创建（发布）新房源 */
export const createHouse = (data) => request.post('/houses', data)

/** 更新指定房源的信息 */
export const updateHouse = (id, data) => request.put(`/houses/${id}`, data)

/** 删除指定房源 */
export const deleteHouse = (id) => request.delete(`/houses/${id}`)

/** 获取当前登录房东发布的房源列表 */
export const getMyHouses = (params) => request.get('/houses/my', { params })

/** 获取首页精选推荐房源（使用公开热门房源接口） */
export const getRecommended = () => request.get('/houses/hot')

/** 按关键词搜索房源（匹配标题、描述、地址） */
export const searchHouses = (params) => request.get('/houses/search', { params })

/** 管理员审核房源，data 包含 { status: 'APPROVED' | 'REJECTED', reason? } */
export const auditHouse = (id, data) => request.put(`/admin/houses/${id}/audit`, data)

/** 收藏房源 */
export const collectHouse = (id) => request.post(`/houses/${id}/collect`)

/** 取消收藏房源 */
export const uncollectHouse = (id) => request.delete(`/houses/${id}/collect`)

/** 获取当前用户收藏的房源列表 */
export const getMyCollections = (params = {}) => request.get('/houses/my/collections', { params })

/** 获取指定房源的图片列表（从 house_images 明细表读取，按排序升序） */
export const getHouseImages = (id) => request.get(`/houses/${id}/images`)

/**
 * 上传房源图片
 *
 * 将图片文件作为 multipart/form-data 发送到后端，
 * 后端将图片保存到磁盘并返回可访问的图片 URL（如 /api/uploads/xxx.jpg）。
 * 使用文件 URL 代替 base64 存储，避免数据库中存储大量二进制数据。
 *
 * @param {File} file - 原始 File 对象（来自 el-upload 的 raw 属性）
 * @returns {Promise<string>} 图片访问 URL
 */
export const uploadHouseImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
