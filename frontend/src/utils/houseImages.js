/**
 * 统一归一化房源图片字段，兼容以下三种输入：
 * 1) 数组：['/api/uploads/a.jpg', ...]
 * 2) JSON 字符串：'["/api/uploads/a.jpg", ...]'
 * 3) 单个 URL 字符串：'/api/uploads/a.jpg'
 *
 * @param {unknown} images 房源图片原始字段
 * @returns {string[]} 清洗后的图片 URL 列表
 */
export function normalizeHouseImages(images) {
  if (Array.isArray(images)) {
    return images.filter(img => typeof img === 'string' && img.trim())
  }
  if (typeof images === 'string') {
    const trimmed = images.trim()
    if (!trimmed) return []
    if (trimmed.startsWith('[')) {
      try {
        const parsed = JSON.parse(trimmed)
        return Array.isArray(parsed) ? parsed.filter(img => typeof img === 'string' && img.trim()) : []
      } catch {
        return []
      }
    }
    return [trimmed]
  }
  return []
}

