/**
 * 统一归一化房源图片字段，兼容以下三种输入：
 * 1) 数组：['/api/uploads/a.jpg', ...]
 * 2) JSON 字符串：'["/api/uploads/a.jpg", ...]'
 * 3) 单个 URL 字符串：'/api/uploads/a.jpg'
 * 4) 逗号分隔字符串：'/api/uploads/a.jpg,/api/uploads/b.jpg'
 *
 * 说明：
 * - 旧数据可能不是标准 JSON，而是“英文逗号/中文逗号分隔”的字符串；
 * - 若不兼容该格式，详情页会误判为仅 1 张图，导致轮播效果不明显。
 *
 * @param {unknown} images 房源图片原始字段
 * @returns {string[]} 清洗后的图片 URL 列表
 */
export function normalizeHouseImages(images) {
  /**
   * 统一清洗 URL 列表：
   * - 去掉空值与空白字符；
   * - 去重，避免上传重复图片导致轮播项重复。
   */
  const sanitize = (arr) => Array.from(
    new Set(
      arr
        .filter(img => typeof img === 'string')
        .map(img => img.trim())
        .filter(Boolean)
    )
  )

  if (Array.isArray(images)) {
    return sanitize(images)
  }
  if (typeof images === 'string') {
    const trimmed = images.trim()
    if (!trimmed) return []
    if (trimmed.startsWith('[')) {
      try {
        const parsed = JSON.parse(trimmed)
        return Array.isArray(parsed) ? sanitize(parsed) : []
      } catch {
        // JSON 解析失败时不直接丢弃，继续按“逗号分隔文本”兜底解析，最大化兼容历史数据。
      }
    }
    // 兼容“英文逗号/中文逗号”分隔的历史图片串
    if (trimmed.includes(',') || trimmed.includes('，')) {
      return sanitize(trimmed.split(/[，,]/))
    }
    return sanitize([trimmed])
  }
  return []
}
