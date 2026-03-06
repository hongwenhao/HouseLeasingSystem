package com.houseleasing.controller;

import com.houseleasing.common.Result;
import com.houseleasing.common.exception.BusinessException;
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

import java.util.Map;

@Tag(name = "User", description = "User management")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @Operation(summary = "Get current user profile")
    @GetMapping("/me")
    public Result<User> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        return Result.success(user);
    }

    @Operation(summary = "Update user profile")
    @PutMapping("/me")
    public Result<User> updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                       @RequestBody UserUpdateRequest request) {
        User user = resolveUser(userDetails.getUsername());
        return Result.success(userService.updateProfile(user.getId(), request));
    }

    @Operation(summary = "Real name authentication")
    @PostMapping("/real-name-auth")
    public Result<Void> realNameAuth(@AuthenticationPrincipal UserDetails userDetails,
                                      @RequestBody Map<String, String> request) {
        User user = resolveUser(userDetails.getUsername());
        userService.realNameAuth(user.getId(), request.get("realName"), request.get("idCard"));
        return Result.success();
    }

    @Operation(summary = "Get user by ID")
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }

    private User resolveUser(String username) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        user.setPassword(null);
        return user;
    }
}
