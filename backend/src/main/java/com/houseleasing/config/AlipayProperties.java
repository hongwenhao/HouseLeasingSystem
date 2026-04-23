package com.houseleasing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 支付宝配置属性
 *
 * <p>集中承载支付宝开放平台（沙箱或正式环境）接入参数，避免在业务代码中硬编码密钥和网关地址。
 * 当前任务按“仅同步回调”方案实现，因此这里重点包含支付网关、应用私钥、公钥和同步回跳地址。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "alipay")
public class AlipayProperties { // 支付宝配置属性载体（由 application.yml 绑定）
    /** 应用 ID（支付宝开放平台分配） */
    private String appId;
    /** 商户应用私钥（PKCS8 格式） */
    private String privateKey;
    /** 支付宝公钥（用于验签） */
    private String publicKey;
    /** 支付网关地址（沙箱通常为 https://openapi-sandbox.dl.alipaydev.com/gateway.do） */
    private String gatewayUrl;
    /** 字符集，默认 utf-8 */
    private String charset = "utf-8";
    /** 签名算法，默认 RSA2 */
    private String signType = "RSA2";
    /** 仅同步回跳地址：用户支付后支付宝重定向到前端页面 */
    private String returnUrl;
    /**
     * 允许作为动态 returnUrl 的主机白名单（不区分大小写）
     *
     * 说明：
     * - 前端可按当前 origin 传回跳地址；
     * - 后端仅允许白名单主机，避免被构造为任意第三方地址。
     */
    private List<String> allowedReturnHosts = new ArrayList<>();
    /**
     * 同步回跳参数数量上限
     *
     * 说明：用于拦截异常大参数包体，默认 40 已覆盖支付宝常见 return_url 参数数量。
     */
    private Integer maxSyncReturnParamCount = 40;
    /** 支付超时时间，示例值 30m（30 分钟） */
    private String timeoutExpress = "30m";
}
