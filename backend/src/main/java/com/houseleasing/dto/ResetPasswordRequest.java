package com.houseleasing.dto;

import lombok.Data;

/**
 * 重置密码请求数据传输对象
 *
 * @author hongwenhao
 * @description 封装忘记密码时重置密码所需的请求参数，通过用户名和手机号验证身份
 */
@Data
public class ResetPasswordRequest {
    /** 用户名 */
    private String username;
    /** 注册时绑定的手机号，用于身份验证 */
    private String phone;
    /** 新密码（明文，后端使用 BCrypt 加密后存储） */
    private String newPassword;
}
