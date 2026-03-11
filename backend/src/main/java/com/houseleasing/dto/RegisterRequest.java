package com.houseleasing.dto;

import lombok.Data;

/**
 * 用户注册请求数据传输对象
 *
 * @author HouseLeasingSystem开发团队
 * @description 封装用户注册所需的请求参数，包括账号信息和用户角色
 */
@Data
public class RegisterRequest {
    /** 注册用户名 */
    private String username;
    /** 手机号码 */
    private String phone;
    /** 电子邮箱 */
    private String email;
    /** 注册密码（明文，后端加密存储） */
    private String password;
    /** 用户角色，默认为租客（TENANT），可选值：TENANT、LANDLORD */
    private String role = "TENANT";
}
