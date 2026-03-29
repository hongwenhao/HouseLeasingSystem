package com.houseleasing.dto;

import lombok.Data;

/**
 * 更新用户信息请求数据传输对象
 *
 * @author HouseLeasingSystem开发团队
 * @description 封装用户修改个人资料所需的请求参数，所有字段均为可选
 */
@Data
public class UserUpdateRequest {
    /** 新的手机号码（可选） */
    private String phone;
    /** 新的电子邮箱（可选） */
    private String email;
    /** 新的头像 URL（可选） */
    private String avatar;
    /** 新的用户名（可选） */
    private String username;
    /** 性别：0-未知，1-男，2-女（可选） */
    private Integer gender;
}
