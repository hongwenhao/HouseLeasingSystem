package com.houseleasing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.houseleasing.common.PageResult;
import com.houseleasing.common.exception.BusinessException;
import com.houseleasing.common.utils.JwtUtil;
import com.houseleasing.dto.ChangePasswordRequest;
import com.houseleasing.dto.LoginRequest;
import com.houseleasing.dto.RegisterRequest;
import com.houseleasing.dto.ResetPasswordRequest;
import com.houseleasing.dto.UserUpdateRequest;
import com.houseleasing.entity.User;
import com.houseleasing.mapper.UserMapper;
import com.houseleasing.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务实现类
 *
 * @author HouseLeasingSystem开发团队
 * @description 实现用户相关的所有业务逻辑，包括注册、登录、信息更新、实名认证、
 *              信用评分管理及管理员对用户的封禁/解封操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * 用户注册：验证用户名、手机号、邮箱的唯一性，加密密码后保存用户信息
     *
     * @param request 注册请求参数（手机号和邮箱均为必填项，且不可重复）
     * @return 注册成功的用户对象（密码字段已清空）
     */
    @Override
    @Transactional
    public User register(RegisterRequest request) {
        // 检查用户名是否已存在
        if (userMapper.selectByUsername(request.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        // 检查手机号唯一性（手机号为必填字段，由 DTO 层 @NotBlank 保证非空）
        if (userMapper.selectByPhone(request.getPhone()) != null) {
            throw new BusinessException("手机号已被注册");
        }
        if (!StringUtils.hasText(request.getEmail())) {
            throw new BusinessException("邮箱不能为空");
        }
        if (userMapper.selectByEmail(request.getEmail()) != null) {
            throw new BusinessException("邮箱已被注册");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        // 使用 BCrypt 加密存储密码
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? request.getRole() : "TENANT");
        user.setCreditScore(100); // 新用户初始信用分 100
        user.setIsRealNameAuth(false);
        user.setStatus("ACTIVE");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        user.setPassword(null); // 清空密码字段后返回
        return user;
    }

    /**
     * 用户登录：验证账号密码，检查账号状态，生成 JWT Token
     *
     * @param request 登录请求参数
     * @return 包含 token 和用户信息的 Map
     */
    @Override
    @Transactional
    public Map<String, Object> login(LoginRequest request) {
        // 支持「用户名」或「手机号」作为统一登录入口，减少前端区分字段的负担
        User user = userMapper.selectByUsernameOrPhone(request.getUsername());
        if (user == null) {
            throw new BusinessException(401, "用户不存在，请确定用户名或手机号是否正确");
        }
        // 验证密码是否匹配
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "密码错误，请重新输入");
        }
        // 检查账号是否被封禁
        if ("BANNED".equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被禁用");
        }
        // 每日登录信用分 +1（同一自然日仅加一次）：
        // 使用数据库原子条件更新，避免并发登录导致同一天重复加分。
        userMapper.addLoginCreditIfNotToday(user.getId());
        // 刷新用户对象，确保返回给前端的是最新信用分/日期
        user = userMapper.selectById(user.getId());
        // 生成 JWT Token（包含用户名和角色）
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        user.setPassword(null); // 清空密码后放入返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);
        return result;
    }

    /**
     * 根据 ID 查询用户信息
     *
     * @param id 用户 ID
     * @return 用户对象（密码字段已清空）
     */
    @Override
    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    /**
     * 更新用户个人资料，仅更新请求中非空的字段
     *
     * @param userId  用户 ID
     * @param request 更新请求参数
     * @return 更新后的用户对象
     */
    @Override
    @Transactional
    public User updateProfile(Long userId, UserUpdateRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        // 仅更新非空字段（支持部分更新）
        if (StringUtils.hasText(request.getPhone())) user.setPhone(request.getPhone());
        if (StringUtils.hasText(request.getEmail())) user.setEmail(request.getEmail());
        if (StringUtils.hasText(request.getAvatar())) user.setAvatar(request.getAvatar());
        if (StringUtils.hasText(request.getUsername())) user.setUsername(request.getUsername());
        if (request.getGender() != null) user.setGender(request.getGender());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        user.setPassword(null);
        return user;
    }

    /**
     * 提交实名认证：保存真实姓名和身份证号，并标记为已实名认证
     *
     * @param userId   用户 ID
     * @param realName 真实姓名
     * @param idCard   身份证号码
     */
    @Override
    @Transactional
    public void realNameAuth(Long userId, String realName, String idCard) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!StringUtils.hasText(realName) || !StringUtils.hasText(idCard)) {
            throw new BusinessException(400, "真实姓名和身份证号不能为空");
        }
        user.setRealName(realName);
        user.setIdCard(idCard);
        user.setIsRealNameAuth(true); // 标记实名认证完成
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    /**
     * 修改用户密码：验证旧密码后使用 BCrypt 加密新密码并更新
     *
     * @param userId  当前登录用户 ID
     * @param request 包含旧密码和新密码的请求对象
     */
    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        // 验证旧密码是否正确
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "旧密码不正确");
        }
        // 使用 BCrypt 加密新密码并更新
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    /**
     * 重置用户密码（忘记密码功能）
     * 通过用户名和手机号验证用户身份，验证通过后使用 BCrypt 加密新密码并更新
     *
     * @param request 包含用户名、手机号和新密码的请求对象
     */
    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userMapper.selectByUsername(request.getUsername());
        // 使用统一的错误信息，防止用户名枚举攻击
        if (user == null || user.getPhone() == null || !user.getPhone().equals(request.getPhone())) {
            throw new BusinessException(400, "用户名或手机号不匹配");
        }
        // 使用 BCrypt 加密新密码并更新
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    /**
     * 更新用户信用评分，分值自动限制在 0-200 范围内
     *
     * @param userId 用户 ID
     * @param delta  评分变动值（正数加分，负数减分）
     */
    @Override
    @Transactional
    public void updateCreditScore(Long userId, int delta) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        // 计算新评分并限制在合法范围 [0, 200]
        int newScore = Math.min(200, Math.max(0, user.getCreditScore() + delta));
        user.setCreditScore(newScore);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    /**
     * 分页查询用户列表，支持按用户名、手机号、邮箱模糊搜索
     *
     * @param page    当前页码
     * @param size    每页大小
     * @param keyword 搜索关键词
     * @return 分页用户列表
     */
    @Override
    public PageResult<User> listUsers(int page, int size, String keyword) {
        Page<User> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            // 支持按用户名、手机号或邮箱进行模糊搜索
            wrapper.like(User::getUsername, keyword)
                    .or().like(User::getPhone, keyword)
                    .or().like(User::getEmail, keyword);
        }
        wrapper.orderByDesc(User::getCreateTime);
        Page<User> result = userMapper.selectPage(pageObj, wrapper);
        // 清空所有用户的密码字段
        result.getRecords().forEach(u -> u.setPassword(null));
        return PageResult.of(result.getTotal(), result.getRecords(), page, size);
    }

    /**
     * 封禁用户账号，设置状态为 BANNED
     *
     * @param userId 要封禁的用户 ID
     */
    @Override
    @Transactional
    public void banUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setStatus("BANNED");
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    /**
     * 解封用户账号，恢复状态为 ACTIVE
     *
     * @param userId 要解封的用户 ID
     */
    @Override
    @Transactional
    public void unbanUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setStatus("ACTIVE");
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }
}
