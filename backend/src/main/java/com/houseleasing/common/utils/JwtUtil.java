package com.houseleasing.common.utils;

import com.houseleasing.security.JwtSubjectConstants;
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
 * @author hongwenhao
 * @description 提供 JWT Token 的生成、验证和解析功能，用于无状态的接口鉴权认证
 */
@Slf4j
@Component // 声明为 JWT 工具组件
public class JwtUtil { // Token 生成、验签与解析实现

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
    public void init() { // 初始化签名密钥对象
        // 使用 HMAC-SHA 算法从密钥字节数组构建签名密钥
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 生成 JWT Token。
     * 说明：用户名是可修改字段，若直接放入 Subject，用户改名后旧 Token 会因“按旧用户名查不到人”而失效。
     * 因此这里将 Subject 设计为稳定不变的用户 ID（带 uid: 前缀），并额外携带 username 便于排查日志。
     *
     * @param userId   用户主键 ID（作为稳定 Subject）
     * @param username 当前用户名（写入 Claims，便于追踪）
     * @param role     用户角色（写入 Claims）
     * @return 生成的 JWT Token 字符串
     */
    public String generateToken(Long userId, String username, String role) { // 生成 JWT Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role); //将用户角色放入 Token，可以直接从中获取用户角色进行权限判断，无需再次查询数据库，提升了效率
        claims.put("username", username); // 用户名用于日志追踪（非认证主键）
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(JwtSubjectConstants.USER_ID_SUBJECT_PREFIX + userId) //设置认证主体使用稳定用户ID
                .setIssuedAt(new Date())  //设置签发时间，记录了 Token 是在何时被创建的
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) //设置过期时间
                .signWith(signingKey, SignatureAlgorithm.HS256)// signingKey: 是服务端的绝密密钥，用于生成和验证签名，使用HMAC-SHA256签名
                .compact();
    }

    /**
     * 验证 JWT Token 是否有效
     *
     * @param token JWT Token 字符串
     * @return true 表示有效，false 表示无效或已过期
     */
    public boolean validateToken(String token) { // 校验 token 是否合法且未过期
        try {
            Jwts.parserBuilder()
                    .setSigningKey(signingKey)//设置签名密钥
                    .build()
                    .parseClaimsJws(token);//计算签名并验证签名和过期时间，如果验证失败会抛出异常
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("无效的 JWT Token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从 JWT Token 中提取 Subject。
     * 新版 Token 返回形如 uid:123 的稳定用户标识；旧版 Token 可能仍是用户名/手机号。
     *
     * @param token JWT Token 字符串
     * @return Subject 原始值
     */
    public String getUsernameFromToken(String token) { // 读取 token 的 subject（兼容 uid:xxx/旧标识）
        return getClaims(token).getSubject();
    }

    /**
     * 从 JWT Token 中提取用户角色
     *
     * @param token JWT Token 字符串
     * @return 用户角色字符串
     */
    public String getRoleFromToken(String token) { // 读取 token 中的角色 claims
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
                .setSigningKey(signingKey)//设置签名密钥
                .build()
                .parseClaimsJws(token)//1.验签 2.自动校验标准字段（如过期时间） 3.解析出Claims载荷
                .getBody();
    }
}
