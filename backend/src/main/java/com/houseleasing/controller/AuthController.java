package com.houseleasing.controller;

import com.houseleasing.common.Result;
import com.houseleasing.dto.LoginRequest;
import com.houseleasing.dto.RegisterRequest;
import com.houseleasing.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Authentication", description = "Auth endpoints")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Operation(summary = "Register new user")
    @PostMapping("/register")
    public Result<Object> register(@RequestBody RegisterRequest request) {
        return Result.success(userService.register(request));
    }

    @Operation(summary = "Login")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        return Result.success(userService.login(request));
    }
}
