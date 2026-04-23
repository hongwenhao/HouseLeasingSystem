/**
 * api/alipay.js —— 支付宝沙箱支付接口
 *
 * 封装“仅同步回调”方案需要的两个核心接口：
 * 1) 发起支付：后端返回支付宝自动提交表单 HTML；
 * 2) 同步回跳验签：前端把 return_url 参数提交给后端验签并确认落库。
 */
import request from './index'

/**
 * 创建支付宝支付表单
 *
 * @param {number} orderId - 待支付订单 ID
 * @param {string} [returnUrl] - 可选的同步回跳地址（建议传当前站点 origin 生成的地址）
 * @returns {Promise<{formHtml: string}>} 表单 HTML
 */
export const createAlipayPayForm = (orderId, returnUrl) => request.post('/alipay/pay/create', { orderId, returnUrl })

/**
 * 校验支付宝同步回调并处理支付结果
 *
 * @param {Record<string, string>} params - return_url query 参数
 * @returns {Promise<{success: boolean, message: string, orderId: number, paymentStatus: string}>}
 */
export const verifyAlipaySyncReturn = (params) => request.post('/alipay/pay/sync/verify', params)
