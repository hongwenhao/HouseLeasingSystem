package com.houseleasing.dto;

import lombok.Data;

/**
 * 用户登录请求数据传输对象
 *
 * @author HouseLeasingSystem开发团队
 * @description 封装用户登录所需的请求参数，包括用户名和密码
 */
@Data
public class LoginRequest {
    /**
     * 登录账号，前端可以填写「用户名」或「手机号」
     * 说明：字段名仍沿用 username，便于兼容现有前端代码，但后端会同时支持手机号匹配
     */
    private String username;
    /** 登录密码（明文，传输后在后端进行验证） */
    private String password;
}
