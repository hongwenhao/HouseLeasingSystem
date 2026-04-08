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
 * @author hongwenhao
 * @description 提供用户个人信息相关的 REST API，包括查询个人信息、更新资料、
 *              实名认证和查询指定用户，所有接口均需要 JWT 认证
 */
@Tag(name = "User", description = "User management")
@RestController
@RequestMapping("/api/users") // 用户中心接口统一前缀
@RequiredArgsConstructor // 自动生成构造函数并注入依赖
@SecurityRequirement(name = "Bearer Authentication")
public class UserController { // 处理个人资料、实名、改密等接口

    private final UserService userService; // 用户业务服务
    private final UserMapper userMapper; // 用户数据库访问组件

    /**
     * 获取当前登录用户的个人信息
     *
     * @param userDetails 当前登录用户信息（由 Spring Security 注入）
     * @return 当前用户的详细信息
     */
    @Operation(summary = "Get current user profile")
    @GetMapping("/me")
    public Result<User> getProfile(@AuthenticationPrincipal UserDetails userDetails) { // 获取当前登录用户资料
        User current = userMapper.selectByUsername(userDetails.getUsername()); // 按登录用户名查当前用户
        if (current == null) { // 登录用户在库中不存在时
            throw new BusinessException(404, "用户不存在"); // 返回“用户不存在”
        }
        // 统一复用服务层查询逻辑，确保用户敏感字段（如身份证）按“存储加密、读取解密”口径返回。
        return Result.success(userService.getUserById(current.getId())); // 返回完整且按规则处理后的用户信息
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
                                       @RequestBody UserUpdateRequest request) { // 更新头像、邮箱、手机号等资料
        User user = resolveUser(userDetails.getUsername()); // 先确认当前用户是谁
        return Result.success(userService.updateProfile(user.getId(), request)); // 调用业务层保存并返回新资料
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
                                      @RequestBody Map<String, String> request) { // 提交实名认证信息
        User user = resolveUser(userDetails.getUsername()); // 获取当前用户ID
        userService.realNameAuth(user.getId(), request.get("realName"), request.get("idCard")); // 保存实名信息并校验
        return Result.success(); // 返回认证提交成功
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
                                        @Valid @RequestBody ChangePasswordRequest request) { // 修改登录密码（需旧密码）
        User user = resolveUser(userDetails.getUsername()); // 定位当前用户
        userService.changePassword(user.getId(), request); // 校验旧密码后写入新密码
        return Result.success(); // 返回修改成功
    }

    /**
     * 根据用户 ID 查询指定用户信息
     *
     * @param id 目标用户 ID
     * @return 目标用户的信息
     */
    @Operation(summary = "Get user by ID")
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) { // 根据用户ID查询资料
        return Result.success(userService.getUserById(id)); // 返回对应用户信息
    }

    /**
     * 根据用户名解析用户信息，并清空密码字段
     *
     * @param username 用户名
     * @return 对应的用户实体（密码已清空）
     */
    private User resolveUser(String username) { // 把登录名解析成用户实体
        User user = userMapper.selectByUsername(username); // 到数据库按用户名查询
        if (user == null) { // 没查询到就表示用户无效
            throw new BusinessException(404, "用户不存在"); // 抛出可理解的提示
        }
        user.setPassword(null); // 清空密码字段，防止密码泄露
        // 出于最小暴露原则，用户中心“/me”接口不直接返回身份证字段（由实名认证流程单独处理）。
        user.setIdCard(null); // 隐藏身份证字段，避免敏感信息暴露
        return user; // 返回已脱敏的用户对象
    }
}
