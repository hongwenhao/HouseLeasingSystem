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
@Tag(name = "Authentication", description = "Auth endpoints") // OpenAPI 文档分组，便于在 Swagger 中定位认证接口
@RestController // 声明为 REST 控制器：返回值会直接序列化为 JSON
@RequestMapping("/api/auth") // 统一认证接口前缀，避免每个方法重复写公共路径
@RequiredArgsConstructor // 为 final 字段生成构造器，交给 Spring 完成依赖注入
public class AuthController { // 认证模块入口：只做参数接收与结果返回，不承载复杂业务

    private final UserService userService; // 认证相关核心业务都下沉在 UserService 中，控制器仅负责转发请求

    /**
     * 用户注册接口
     *
     * @param request 注册请求参数（用户名、密码、手机号等）
     * @return 注册成功的用户信息
     */
    @Operation(summary = "Register new user") // Swagger 文档说明：该接口用于新用户注册
    @PostMapping("/register") // HTTP POST /api/auth/register
    public Result<Object> register(@Valid @RequestBody RegisterRequest request) { // @Valid 触发参数校验，非法请求会被全局异常处理器拦截
        return Result.success(userService.register(request)); // 执行注册并返回统一 Result 包装，前端按统一结构解析
    }

    /**
     * 用户登录接口，验证成功后返回 JWT Token
     *
     * @param request 登录请求参数（用户名和密码）
     * @return 包含 token 和用户信息的登录结果
     */
    @Operation(summary = "Login") // Swagger 文档说明：该接口用于账号登录
    @PostMapping("/login") // HTTP POST /api/auth/login
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) { // 读取登录凭证（用户名/手机号 + 密码）
        return Result.success(userService.login(request)); // 登录成功后返回 token 与用户信息，供前端保存登录态
    }

    /**
     * 重置密码接口（忘记密码），通过用户名和手机号验证身份后重置密码
     *
     * @param request 重置密码请求参数（用户名、手机号、新密码）
     * @return 操作成功的响应
     */
    @Operation(summary = "Reset password") // Swagger 文档说明：该接口用于忘记密码后的重置
    @PostMapping("/reset-password") // HTTP POST /api/auth/reset-password
    public Result<Void> resetPassword(@RequestBody ResetPasswordRequest request) { // 接收“用户名+手机号+新密码”作为重置依据
        userService.resetPassword(request); // 服务层会校验身份匹配关系并落库更新密码
        return Result.success(); // 重置成功只返回状态，不附带业务数据
    }
}
