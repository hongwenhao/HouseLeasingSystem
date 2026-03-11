package com.houseleasing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * @author HouseLeasingSystem开发团队
 * @description 对应数据库 users 表，存储系统用户的基本信息，
 *              包括普通租客、房东和管理员三种角色
 */
@Data
@TableName("users")
public class User {
    /** 用户主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户名（登录账号） */
    private String username;
    /** 手机号码 */
    private String phone;
    /** 电子邮箱 */
    private String email;
    /** 加密后的密码 */
    private String password;
    /** 用户角色：TENANT（租客）、LANDLORD（房东）、ADMIN（管理员） */
    private String role;
    /** 真实姓名（实名认证后填写） */
    private String realName;
    /** 身份证号码（实名认证后填写） */
    private String idCard;
    /** 信用评分，默认 100 分，范围 0-200 */
    private Integer creditScore = 100;
    /** 是否已完成实名认证，对应数据库字段 is_real_name_auth */
    private Boolean isRealNameAuth = false; // maps to is_real_name_auth
    /** 账号状态：ACTIVE（正常）、BANNED（封禁） */
    private String status = "ACTIVE";
    /** 用户头像 URL */
    private String avatar;
    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /** 更新时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
