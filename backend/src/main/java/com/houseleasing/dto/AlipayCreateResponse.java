package com.houseleasing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 发起支付宝支付响应 DTO
 *
 * <p>返回可直接提交到支付宝收银台的 HTML 表单字符串，前端写入页面后自动提交即可跳转支付。</p>
 */
@Data
@AllArgsConstructor
public class AlipayCreateResponse {
    /** 支付宝官方生成的自动提交表单 HTML */
    private String formHtml;
}

