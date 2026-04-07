package com.houseleasing.security;

import com.houseleasing.common.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 *
 * @author hongwenhao
 * @description 每次 HTTP 请求执行一次的过滤器，从请求头中提取 JWT Token，
 *              验证有效性后将认证信息注入 Spring Security 上下文
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    /**
     * 过滤器核心逻辑：解析 Authorization 请求头中的 Bearer Token 并设置认证信息
     *
     * @param request     HTTP 请求对象
     * @param response    HTTP 响应对象
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        // 仅处理 Bearer Token 格式的认证请求
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // 截取 "Bearer " 后的 Token 部分
            try {
                if (jwtUtil.validateToken(token)) {
                    // Token 有效，从 Token 中提取用户名
                    String username = jwtUtil.getUsernameFromToken(token);
                    // 通过 UserDetailsService 加载完整的 UserDetails 对象，
                    // 确保 @AuthenticationPrincipal 能正确注入 UserDetails
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    // 构建 Spring Security 认证对象
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    // 将认证信息注入 Security 上下文，后续的权限验证将使用此信息
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                log.warn("JWT processing failed: {}", e.getMessage());
            }
        }
        // 无论认证是否成功，都继续执行后续过滤器
        filterChain.doFilter(request, response);
    }
}
