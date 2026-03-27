package com.houseleasing.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 *
 * @author HouseLeasingSystem开发团队
 * @description 提供 JWT Token 的生成、验证和解析功能，用于无状态的接口鉴权认证
 */
@Slf4j
@Component
public class JwtUtil {

    /** JWT 签名密钥（从配置文件读取） */
    @Value("${jwt.secret}")
    private String secret;

    /** JWT Token 的过期时间（毫秒，从配置文件读取） */
    @Value("${jwt.expiration}")
    private long expiration;

    /** HMAC 签名密钥对象 */
    private Key signingKey;

    /**
     * 初始化方法，在 Bean 实例化后将密钥字符串转换为 Key 对象
     */
    @PostConstruct
    public void init() {
        // 使用 HMAC-SHA 算法从密钥字节数组构建签名密钥
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 生成 JWT Token
     *
     * @param username 用户名（作为 Subject）
     * @param role     用户角色（存入 Claims）
     * @return 生成的 JWT Token 字符串
     */
    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role); // 将角色信息写入 JWT 载荷
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)                                           // 设置主题为用户名
                .setIssuedAt(new Date())                                        // 签发时间
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // 过期时间
                .signWith(signingKey, SignatureAlgorithm.HS256)                 // 使用 HMAC-SHA256 签名
                .compact();
    }

    /**
     * 验证 JWT Token 是否有效
     *
     * @param token JWT Token 字符串
     * @return true 表示有效，false 表示无效或已过期
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("无效的 JWT Token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从 JWT Token 中提取用户名
     *
     * @param token JWT Token 字符串
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * 从 JWT Token 中提取用户角色
     *
     * @param token JWT Token 字符串
     * @return 用户角色字符串
     */
    public String getRoleFromToken(String token) {
        return (String) getClaims(token).get("role");
    }

    /**
     * 解析 JWT Token 并返回 Claims 载荷
     *
     * @param token JWT Token 字符串
     * @return JWT Claims 对象
     */
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
