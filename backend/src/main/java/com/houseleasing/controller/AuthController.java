package com.houseleasing.controller;

import com.houseleasing.common.Result;
import com.houseleasing.dto.LoginRequest;
import com.houseleasing.dto.RegisterRequest;
import com.houseleasing.dto.ResetPasswordRequest;
import com.houseleasing.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 *
 * @author hongwenhao
 * @description 处理用户注册和登录请求，提供公开访问的认证相关接口
 */
@Tag(name = "Authentication", description = "Auth endpoints")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 用户注册接口
     *
     * @param request 注册请求参数（用户名、密码、手机号等）
     * @return 注册成功的用户信息
     */
    @Operation(summary = "Register new user")
    @PostMapping("/register")
    public Result<Object> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(userService.register(request));
    }

    /**
     * 用户登录接口，验证成功后返回 JWT Token
     *
     * @param request 登录请求参数（用户名和密码）
     * @return 包含 token 和用户信息的登录结果
     */
    @Operation(summary = "Login")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        return Result.success(userService.login(request));
    }

    /**
     * 重置密码接口（忘记密码），通过用户名和手机号验证身份后重置密码
     *
     * @param request 重置密码请求参数（用户名、手机号、新密码）
     * @return 操作成功的响应
     */
    @Operation(summary = "Reset password")
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request);
        return Result.success();
    }
}
