package com.houseleasing.common.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码工具类
 *
 * @author hongwenhao
 * @description 封装 Spring Security 的 PasswordEncoder，提供密码加密和匹配功能，
 *              使用 BCrypt 算法对用户密码进行单向哈希加密
 */
@Component // 声明为密码工具组件
@RequiredArgsConstructor
public class PasswordUtil { // 密码加密与匹配能力封装

    /** Spring Security 密码编码器（BCrypt 实现） */
    private final PasswordEncoder passwordEncoder; // Spring Security 密码编码器

    /**
     * 对明文密码进行加密
     *
     * @param rawPassword 明文密码
     * @return 加密后的密码哈希值
     */
    public String encode(String rawPassword) { // 对明文密码做单向加密
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * 验证明文密码与加密密码是否匹配
     *
     * @param rawPassword     明文密码
     * @param encodedPassword 已加密的密码哈希值
     * @return true 表示密码匹配，false 表示不匹配
     */
    public boolean matches(String rawPassword, String encodedPassword) { // 校验明文与密文是否匹配
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
