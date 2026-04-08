package com.houseleasing.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝客户端配置
 *
 * 创建并注入统一的 {@link AlipayClient}，供支付服务调用“统一收单下单接口”和“同步回调验签”。
 * 配置校验在业务调用时执行，避免未配置支付宝时影响系统其余功能启动。</p>
 */
@Configuration
@RequiredArgsConstructor
public class AlipayConfig {

    private final AlipayProperties alipayProperties;

    /**
     * 构建支付宝客户端
     *
     * @return 可复用的支付宝 SDK 客户端
     */
    @Bean
    public AlipayClient alipayClient() {
        return new DefaultAlipayClient(
                alipayProperties.getGatewayUrl(),//网关地址
                alipayProperties.getAppId(),//应用身份
                alipayProperties.getPrivateKey(),//商户私钥
                "json",//数据格式
                alipayProperties.getCharset(),//字符编码
                alipayProperties.getPublicKey(),//支付宝公钥
                alipayProperties.getSignType()//签名算法
        );
    }
}
