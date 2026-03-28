package com.houseleasing.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 用户注册请求数据传输对象
 *
 * @author HouseLeasingSystem开发团队
 * @description 封装用户注册所需的请求参数，包括账号信息和用户角色。
 *              手机号、邮箱均为必填项且在数据库中保持唯一约束。
 */
@Data
public class RegisterRequest {
    /** 注册用户名 */
    private String username;

    /**
     * 手机号码（必填，格式校验：1开头的11位国内手机号）
     * 在数据库 users.phone 列上有 UNIQUE 约束，注册时也会进行唯一性检查
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 电子邮箱（必填，格式校验）
     * 在数据库 users.email 列上有 UNIQUE 约束，注册时也会进行唯一性检查
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 注册密码（明文，后端使用 BCrypt 加密后存储） */
    private String password;

    /** 用户角色，默认为租客（TENANT），可选值：TENANT、LANDLORD */
    private String role = "TENANT";
}
