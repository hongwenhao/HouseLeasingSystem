package com.houseleasing.config;

import com.houseleasing.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 安全配置类
 *
 * @author HouseLeasingSystem开发团队
 * @description 配置系统的安全策略，包括 JWT 过滤器集成、接口权限规则、CORS 跨域配置、
 *              无状态 Session 策略和密码编码器
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // 启用方法级别的权限控制（@PreAuthorize 等注解）
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 配置 HTTP 安全策略和过滤器链
     * 包括 CSRF 禁用、CORS 配置、Session 无状态、接口权限规则和 JWT 过滤器
     *
     * @param http HttpSecurity 配置对象
     * @return 构建完成的安全过滤器链
     * @throws Exception 配置失败时抛出
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF disabled intentionally: this is a stateless REST API using JWT Bearer tokens.
            // CSRF protection is only needed for session/cookie-based authentication.
            .csrf(AbstractHttpConfigurer::disable) // 禁用 CSRF，REST API 使用 JWT 无需 CSRF 保护
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // 应用 CORS 配置
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 无状态 Session
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll() // 注册和登录接口公开访问
                .requestMatchers(HttpMethod.GET, "/api/houses", "/api/houses/search").permitAll() // 房源列表和搜索公开
                .requestMatchers(HttpMethod.GET, "/api/houses/{id}").permitAll() // 房源详情公开
                .requestMatchers(HttpMethod.GET, "/api/uploads/**").permitAll() // 已上传的图片文件公开访问（无需登录即可查看房源图片）
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll() // Swagger 文档公开
                .anyRequest().authenticated() // 其他所有接口需要认证
            )
            // 将 JWT 过滤器添加到 UsernamePasswordAuthenticationFilter 之前
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 配置 CORS 跨域策略，允许所有来源、方法和请求头
     *
     * @return CORS 配置来源对象
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*")); // 允许所有来源（生产环境应限制为具体域名）
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*")); // 允许所有请求头
        config.setAllowCredentials(true); // 允许携带凭证（Cookie/Authorization）
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // 对所有路径应用 CORS 配置
        return source;
    }

    /**
     * 配置密码编码器，使用 BCrypt 算法
     *
     * @return BCrypt 密码编码器实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置认证管理器
     *
     * @param config 认证配置对象
     * @return 认证管理器实例
     * @throws Exception 获取失败时抛出
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
