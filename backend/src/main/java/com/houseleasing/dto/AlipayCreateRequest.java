package com.houseleasing.dto;

import lombok.Data;

/**
 * 发起支付宝支付请求 DTO
 *
 * <p>当前仅需要订单 ID，后台会基于订单自动计算支付金额并生成支付宝页面跳转表单。</p>
 */
@Data
public class AlipayCreateRequest {
    /** 待支付订单 ID */
    private Long orderId;
}

