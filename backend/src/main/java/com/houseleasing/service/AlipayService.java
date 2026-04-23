package com.houseleasing.service;

import com.houseleasing.dto.AlipaySyncVerifyResponse;

import java.util.Map;

/**
 * 支付宝支付服务接口
 * 封装租客订单使用支付宝沙箱支付的核心能力：
 * 1) 生成跳转支付宝收银台的支付表单；
 * 2) 处理并校验同步回调参数，完成订单支付状态落库。</p>
 */
public interface AlipayService { // 支付宝能力抽象：定义“创建支付单”和“回调验签”两个核心动作

    /**
     * 生成支付宝支付页面表单 HTML
     *
     * @param orderId  订单 ID
     * @param tenantId 当前租客 ID（权限校验）
     * @param returnUrl 前端传入的同步回跳地址（可选，非法时由实现层回退默认配置）
     * @return 自动提交到支付宝的 HTML 表单
     */
    String createPayForm(Long orderId, Long tenantId, String returnUrl); // 生成前端可直接提交到支付宝的 HTML 表单

    /**
     * 校验并处理支付宝同步回调
     *
     * @param params 同步回调 query 参数
     * @return 验签和处理结果
     */
    AlipaySyncVerifyResponse verifyAndHandleSyncReturn(Map<String, String> params); // 校验支付宝回调并回写订单支付结果
}
