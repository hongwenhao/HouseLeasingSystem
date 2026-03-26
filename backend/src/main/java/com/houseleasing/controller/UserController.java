package com.houseleasing.controller;

import com.houseleasing.common.Result;
import com.houseleasing.common.exception.BusinessException;
import com.houseleasing.dto.ChangePasswordRequest;
import com.houseleasing.dto.UserUpdateRequest;
import com.houseleasing.entity.User;
import com.houseleasing.mapper.UserMapper;
import com.houseleasing.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * 用户管理控制器
 *
 * @author HouseLeasingSystem开发团队
 * @description 提供用户个人信息相关的 REST API，包括查询个人信息、更新资料、
 *              实名认证和查询指定用户，所有接口均需要 JWT 认证
 */
@Tag(name = "User", description = "User management")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    /**
     * 获取当前登录用户的个人信息
     *
     * @param userDetails 当前登录用户信息（由 Spring Security 注入）
     * @return 当前用户的详细信息
     */
    @Operation(summary = "Get current user profile")
    @GetMapping("/me")
    public Result<User> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        return Result.success(user);
    }

    /**
     * 更新当前用户的个人资料
     *
     * @param userDetails 当前登录用户信息
     * @param request     包含要更新字段的请求对象（手机、邮箱、头像、用户名）
     * @return 更新后的用户信息
     */
    @Operation(summary = "Update user profile")
    @PutMapping("/me")
    public Result<User> updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                       @RequestBody UserUpdateRequest request) {
        User user = resolveUser(userDetails.getUsername());
        return Result.success(userService.updateProfile(user.getId(), request));
    }

    /**
     * 提交实名认证信息（真实姓名和身份证号）
     *
     * @param userDetails 当前登录用户信息
     * @param request     包含 realName 和 idCard 的请求体
     * @return 操作成功的响应
     */
    @Operation(summary = "Real name authentication")
    @PostMapping("/real-name-auth")
    public Result<Void> realNameAuth(@AuthenticationPrincipal UserDetails userDetails,
                                      @RequestBody Map<String, String> request) {
        User user = resolveUser(userDetails.getUsername());
        userService.realNameAuth(user.getId(), request.get("realName"), request.get("idCard"));
        return Result.success();
    }

    /**
     * 修改当前用户密码（需验证旧密码）
     *
     * @param userDetails 当前登录用户信息
     * @param request     包含旧密码和新密码的请求对象
     * @return 操作成功的响应
     */
    @Operation(summary = "Change user password")
    @PutMapping("/password")
    public Result<Void> changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                        @Valid @RequestBody ChangePasswordRequest request) {
        User user = resolveUser(userDetails.getUsername());
        userService.changePassword(user.getId(), request);
        return Result.success();
    }

    /**
     * 根据用户 ID 查询指定用户信息
     *
     * @param id 目标用户 ID
     * @return 目标用户的信息
     */
    @Operation(summary = "Get user by ID")
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }

    /**
     * 根据用户名解析用户信息，并清空密码字段
     *
     * @param username 用户名
     * @return 对应的用户实体（密码已清空）
     */
    private User resolveUser(String username) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        user.setPassword(null); // 清空密码字段，防止密码泄露
        return user;
    }
}
