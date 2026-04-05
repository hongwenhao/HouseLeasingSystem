package com.houseleasing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 支付宝同步回调验签结果 DTO
 *
 * <p>前端在 return_url 页面调用后端验签接口，后端返回验签与业务落库结果，前端据此提示用户。</p>
 */
@Data
@AllArgsConstructor
public class AlipaySyncVerifyResponse {
    /** 是否验签且业务确认成功 */
    private boolean success;
    /** 验签/处理结果提示文案 */
    private String message;
    /** 订单 ID */
    private Long orderId;
    /** 订单当前支付状态（UNPAID/PAID/REFUNDED） */
    private String paymentStatus;
}

