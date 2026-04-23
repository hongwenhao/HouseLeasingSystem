package com.houseleasing.dto;

import lombok.Data;

/**
 * 发起支付宝支付请求 DTO
 *
 * <p>除订单 ID 外，前端还会携带当前页面 origin 生成的 returnUrl，
 * 以解决“用户用 127.0.0.1 登录、但配置回跳到 localhost 导致登录态丢失”的问题。</p>
 */
@Data
public class AlipayCreateRequest {
    /** 待支付订单 ID */
    private Long orderId;
    /**
     * 前端希望使用的同步回跳地址（可选）
     *
     * 说明：
     * 1) 前端会基于当前 window.location.origin 生成该值，确保支付后回到“同源站点”；
     * 2) 后端会做基础合法性校验，非法值将自动回退为配置文件中的默认 returnUrl。
     */
    private String returnUrl;
}
