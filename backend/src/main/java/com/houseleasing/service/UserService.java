package com.houseleasing.service;

import com.houseleasing.common.PageResult;
import com.houseleasing.dto.ChangePasswordRequest;
import com.houseleasing.dto.LoginRequest;
import com.houseleasing.dto.RegisterRequest;
import com.houseleasing.dto.ResetPasswordRequest;
import com.houseleasing.dto.UserUpdateRequest;
import com.houseleasing.entity.User;

import java.util.Map;

/**
 * 用户服务接口
 *
 * @author HouseLeasingSystem开发团队
 * @description 定义用户相关的业务操作，包括注册、登录、信息管理、实名认证及管理员操作
 */
public interface UserService {

    /**
     * 用户注册
     *
     * @param request 注册请求参数（用户名、密码、手机号等）
     * @return 注册成功的用户对象（密码字段已清空）
     */
    User register(RegisterRequest request);

    /**
     * 用户登录，验证账号密码并生成 JWT Token
     *
     * @param request 登录请求参数（用户名和密码）
     * @return 包含 token 和用户信息的 Map
     */
    Map<String, Object> login(LoginRequest request);

    /**
     * 根据用户 ID 查询用户信息
     *
     * @param id 用户 ID
     * @return 用户对象（密码字段已清空）
     */
    User getUserById(Long id);

    /**
     * 更新用户个人资料
     *
     * @param userId  要更新的用户 ID
     * @param request 包含更新内容的请求对象
     * @return 更新后的用户对象
     */
    User updateProfile(Long userId, UserUpdateRequest request);

    /**
     * 提交用户实名认证信息
     *
     * @param userId   用户 ID
     * @param realName 真实姓名
     * @param idCard   身份证号码
     */
    void realNameAuth(Long userId, String realName, String idCard);

    /**
     * 修改用户密码（需验证旧密码）
     *
     * @param userId  当前登录用户 ID
     * @param request 包含旧密码和新密码的请求对象
     */
    void changePassword(Long userId, ChangePasswordRequest request);

    /**
     * 重置用户密码（忘记密码功能）
     * 通过用户名和手机号验证用户身份，验证通过后更新密码
     *
     * @param request 包含用户名、手机号和新密码的请求对象
     */
    void resetPassword(ResetPasswordRequest request);

    /**
     * 更新用户信用评分
     *
     * @param userId 用户 ID
     * @param delta  评分变动值（正数加分，负数减分），结果限制在 0-200 之间
     */
    void updateCreditScore(Long userId, int delta);

    /**
     * 分页查询用户列表（管理员使用）
     *
     * @param page    当前页码
     * @param size    每页大小
     * @param keyword 搜索关键词（匹配用户名、手机号、邮箱）
     * @return 分页用户列表
     */
    PageResult<User> listUsers(int page, int size, String keyword);

    /**
     * 封禁指定用户账号
     *
     * @param userId 要封禁的用户 ID
     */
    void banUser(Long userId);

    /**
     * 解封指定用户账号
     *
     * @param userId 要解封的用户 ID
     */
    void unbanUser(Long userId);
}
