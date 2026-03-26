package com.houseleasing.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改密码请求数据传输对象
 *
 * @author HouseLeasingSystem开发团队
 * @description 封装已登录用户修改密码所需的请求参数，需提供旧密码进行身份验证
 */
@Data
public class ChangePasswordRequest {
    /** 当前密码（旧密码），用于身份验证 */
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;
    /** 新密码（明文，后端使用 BCrypt 加密后存储） */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, message = "密码至少6位")
    private String newPassword;
}
