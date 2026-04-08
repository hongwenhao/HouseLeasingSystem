package com.houseleasing.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 身份证号加解密服务（数据库静态存储加密）。
 * 设计目标：
 * 1) 入库前加密（防止明文身份证号落库）；
 * 2) 出库后按需解密（兼容既有业务展示与脱敏流程）；
 */
@Slf4j
@Component // 声明为身份证加解密组件
@lombok.RequiredArgsConstructor
public class IdCardCryptoService { // 身份证入库加密/出库解密实现

    /**
     * 默认演示密钥（仅开发环境可用）。
     */
    private static final String DEFAULT_SECRET = "HouseLeasingSystem-IdCard-Default-Secret-ChangeMe";
    /**
     * 密文前缀：用于快速区分“已加密数据”与“历史明文数据”。
     */
    private static final String ENCRYPTED_PREFIX = "ENC$";
    /**
     * AES-GCM 推荐 12 字节 IV。
     */
    private static final int GCM_IV_LENGTH = 12;
    /**
     * GCM 认证标签长度（bit）。
     */
    private static final int GCM_TAG_LENGTH_BIT = 128;
    private static final String AES_ALGO = "AES";
    private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";

    /**
     * 身份证加密主密钥（建议通过环境变量覆盖）：
     * - 生产环境务必配置强密钥并定期轮换；
     * - 默认值仅用于本地开发演示，避免无配置时应用启动失败。
     */
    @Value("${app.security.id-card-secret:" + DEFAULT_SECRET + "}")
    private String idCardSecret;

    private final SecureRandom secureRandom = new SecureRandom(); // 安全随机数生成器（用于 IV）
    private final Environment environment; // 运行环境信息（用于生产环境密钥校验）

    /**
     * 启动时执行安全兜底：
     * - 生产环境若仍使用默认密钥，直接拒绝启动；
     * - 防止“默认弱密钥误上线”导致的敏感数据可预测解密风险。
     */
    @jakarta.annotation.PostConstruct
    public void validateSecret() { // 启动时校验密钥安全性
        if (!DEFAULT_SECRET.equals(idCardSecret)) {
            return;
        }
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                throw new IllegalStateException("生产环境禁止使用默认身份证加密密钥，请配置 app.security.id-card-secret");
            }
        }
    }

    /**
     * 入库加密：
     * - 若传入为空，直接返回原值；
     * - 若已是密文格式（带前缀），直接返回（幂等保护）；
     * - 其他情况执行 AES-GCM 加密并返回前缀密文。
     */
    public String encryptForStorage(String plainIdCard) { // 将身份证号加密为可存储密文
        if (!StringUtils.hasText(plainIdCard)) {
            return plainIdCard;
        }
        String normalized = plainIdCard.trim().toUpperCase();
        if (isEncrypted(normalized)) {
            return normalized;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, buildSecretKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BIT, iv));
            byte[] encrypted = cipher.doFinal(normalized.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception ex) {
            throw new IllegalStateException("身份证号加密失败", ex);
        }
    }

    /**
     * 出库解密：
     * - 为空时原样返回；
     * - 非密文格式视为历史明文，直接返回；
     * - 密文格式按 AES-GCM 解密；
     * - 若解密失败，记录告警并返回原值，避免影响历史脏数据读取链路。
     */
    public String decryptFromStorage(String storedIdCard) { // 将存储密文解密为明文
        if (!StringUtils.hasText(storedIdCard)) {
            return storedIdCard;
        }
        String trimmed = storedIdCard.trim();
        if (!isEncrypted(trimmed)) {
            return trimmed;
        }
        try {
            byte[] all = Base64.getDecoder().decode(trimmed.substring(ENCRYPTED_PREFIX.length()));
            if (all.length <= GCM_IV_LENGTH) {
                return trimmed;
            }
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[all.length - GCM_IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(all, GCM_IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, buildSecretKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BIT, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.warn("身份证号解密失败，返回原值以兼容历史数据读取：{}", ex.getMessage());
            return trimmed;
        }
    }

    private boolean isEncrypted(String value) {
        return value != null && value.startsWith(ENCRYPTED_PREFIX);
    }

    /**
     * 由配置密钥派生 AES 对称密钥：
     * - 使用 SHA-256 做固定长度派生；
     * - 直接使用 32 字节摘要作为 AES-256 key。
     */
    private SecretKeySpec buildSecretKey() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(idCardSecret.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, AES_ALGO);
    }
}
