package com.houseleasing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.houseleasing.common.PageResult;
import com.houseleasing.common.security.IdCardCryptoService;
import com.houseleasing.common.exception.BusinessException;
import com.houseleasing.common.utils.JwtUtil;
import com.houseleasing.dto.ChangePasswordRequest;
import com.houseleasing.dto.LoginRequest;
import com.houseleasing.dto.RegisterRequest;
import com.houseleasing.dto.ResetPasswordRequest;
import com.houseleasing.dto.UserUpdateRequest;
import com.houseleasing.entity.User;
import com.houseleasing.mapper.UserMapper;
import com.houseleasing.mq.MessageProducer;
import com.houseleasing.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现类
 *
 * @author hongwenhao
 * @description 实现用户相关的所有业务逻辑，包括注册、登录、信息更新、实名认证、
 *              信用评分管理及管理员对用户的封禁/解封操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /** 每日首次登录可获得的信用分奖励 */
    private static final int DAILY_LOGIN_CREDIT_BONUS = 1;
    /** 信用分系统上限 */
    private static final int CREDIT_SCORE_MAX = 200;
    /** 每日首次登录提醒文案模板 */
    private static final String DAILY_LOGIN_BONUS_MESSAGE_TEMPLATE = "今日登录成功，信用分+%d（每日仅首次登录生效）";
    /** 非首次登录提醒文案 */
    private static final String DAILY_LOGIN_REPEAT_MESSAGE = "登录成功（今日已完成登录加分，不重复累计）";
    /** 中国大陆身份证号基础格式：18 位，前 17 位数字 + 末位数字或 X */
    private static final String CHINA_ID_CARD_REGEX = "^\\d{17}[\\dXx]$";
    /** 身份证校验位计算权重（国家标准 GB 11643） */
    private static final int[] ID_CARD_WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    /** 校验位映射表：sum % 11 后对应字符 */
    private static final char[] ID_CARD_CHECK_CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MessageProducer messageProducer;
    private final IdCardCryptoService idCardCryptoService;

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
        // 使用 Redis SETNX + 日期维度 key 去重，key 过期时间固定 1 天。
        String loginCreditKey = "credit:login:" + user.getId() + ":" + LocalDate.now();
        Boolean firstLoginToday = redisTemplate.opsForValue().setIfAbsent(loginCreditKey, "1", 1, TimeUnit.DAYS);
        if (Boolean.TRUE.equals(firstLoginToday)) {
            int currentScore = user.getCreditScore() == null ? 0 : user.getCreditScore();
            user.setCreditScore(Math.min(CREDIT_SCORE_MAX, currentScore + DAILY_LOGIN_CREDIT_BONUS));
            user.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(user);
            // 每日首次登录时，向消息中心推送一条“登录成功”通知：
            // 1) 明确告知用户本次登录已记录；
            // 2) 同步提示“每日登录加分”规则，帮助用户理解信用分变动来源；
            // 3) 仅首次登录发送，避免同一天内重复提醒造成打扰。
            messageProducer.sendLoginNotification(user.getId(),
                    String.format(DAILY_LOGIN_BONUS_MESSAGE_TEMPLATE, DAILY_LOGIN_CREDIT_BONUS));
        } else {
            // 非每日首次登录也保留登录通知，便于用户在消息中心追溯关键登录行为；
            // 文案明确“已登录但不重复加分”，避免用户误解积分规则。
            messageProducer.sendLoginNotification(user.getId(), DAILY_LOGIN_REPEAT_MESSAGE);
        }
        // 生成 JWT Token：
        // 1) Subject 使用稳定 userId，避免“用户名修改后旧 Token 立即失效”；
        // 2) Claims 保留当前用户名与角色，便于审计与问题排查。
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
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
        // 对外返回用户信息时按需解密身份证字段，确保前端表单“已认证信息回显”不受存储加密影响。
        user.setIdCard(idCardCryptoService.decryptFromStorage(user.getIdCard()));
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
        if (StringUtils.hasText(request.getPhone())) {
            User existingByPhone = userMapper.selectByPhone(request.getPhone());
            if (existingByPhone != null && !existingByPhone.getId().equals(userId)) {
                throw new BusinessException("手机号已被注册");
            }
            user.setPhone(request.getPhone());
        }
        if (StringUtils.hasText(request.getEmail())) {
            User existingByEmail = userMapper.selectByEmail(request.getEmail());
            if (existingByEmail != null && !existingByEmail.getId().equals(userId)) {
                throw new BusinessException("邮箱已被注册");
            }
            user.setEmail(request.getEmail());
        }
        if (StringUtils.hasText(request.getAvatar())) user.setAvatar(request.getAvatar());
        if (StringUtils.hasText(request.getUsername())) {
            User existingByUsername = userMapper.selectByUsername(request.getUsername());
            if (existingByUsername != null && !existingByUsername.getId().equals(userId)) {
                throw new BusinessException("用户名已存在");
            }
            user.setUsername(request.getUsername());
        }
        if (request.getGender() != null) user.setGender(request.getGender());
        user.setUpdateTime(LocalDateTime.now());
        try {
            userMapper.updateById(user);
        } catch (DuplicateKeyException e) {
            String message = e.getMessage();
            if (message != null) {
                if (message.contains("users.username")) {
                    throw new BusinessException("用户名已存在");
                }
                if (message.contains("users.phone")) {
                    throw new BusinessException("手机号已被注册");
                }
                if (message.contains("users.email")) {
                    throw new BusinessException("邮箱已被注册");
                }
            }
            throw new BusinessException("资料更新失败，存在重复数据");
        }
        user.setPassword(null);
        // 更新资料接口复用用户对象直接返回，需要解密身份证以保持接口输出语义稳定。
        user.setIdCard(idCardCryptoService.decryptFromStorage(user.getIdCard()));
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
        // 入参统一去空白，避免用户输入前后空格导致“看起来一样但校验失败/存储不一致”。
        String normalizedRealName = realName.trim();
        String normalizedIdCard = idCard.trim().toUpperCase();
        // 实名认证时强制身份证号校验，防止无效证件号写入系统影响合同与风控流程。
        validateChineseIdCard(normalizedIdCard);

        user.setRealName(normalizedRealName);
        // 身份证号入库前做对称加密，避免敏感明文直接落库。
        user.setIdCard(idCardCryptoService.encryptForStorage(normalizedIdCard));
        user.setIsRealNameAuth(true); // 标记实名认证完成
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    /**
     * 校验中国大陆身份证号：
     * 1) 基础正则：18 位，前 17 位数字 + 最后一位数字/X；
     * 2) 出生日期段有效性：yyyyMMdd 必须是合法自然日；
     * 3) 校验位正确性：按 GB 11643 算法验证第 18 位。
     * 不通过时抛出统一业务异常，便于前端提示用户修正输入。
     */
    private void validateChineseIdCard(String idCard) {
        if (!idCard.matches(CHINA_ID_CARD_REGEX)) {
            throw new BusinessException(400, "身份证号格式不正确，请输入18位身份证号");
        }
        String birthText = idCard.substring(6, 14);
        if (!isValidBirthDate(birthText)) {
            throw new BusinessException(400, "身份证号中的出生日期无效");
        }
        char expectedCheckCode = calculateIdCardCheckCode(idCard.substring(0, 17));
        if (idCard.charAt(17) != expectedCheckCode) {
            throw new BusinessException(400, "身份证号校验位不正确");
        }
    }

    /**
     * 身份证出生日期段校验：
     * - 仅接受 8 位数字 yyyyMMdd；
     * - 通过 LocalDate.of 做严格日期校验（含闰年规则）；
     * - 非法日期返回 false，不抛异常，交由上层统一抛业务异常。
     */
    private boolean isValidBirthDate(String birthText) {
        if (!birthText.matches("\\d{8}")) {
            return false;
        }
        try {
            int year = Integer.parseInt(birthText.substring(0, 4));
            int month = Integer.parseInt(birthText.substring(4, 6));
            int day = Integer.parseInt(birthText.substring(6, 8));
            LocalDate.of(year, month, day);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /**
     * 依据身份证前 17 位计算校验位（GB 11643）：
     * 1) 每一位数字与固定权重相乘并求和；
     * 2) 对 11 取模；
     * 3) 用映射表得到最终校验位字符。
     */
    private char calculateIdCardCheckCode(String base17) {
        int sum = 0;
        for (int i = 0; i < base17.length(); i++) {
            int digit = Character.digit(base17.charAt(i), 10);
            if (digit < 0) {
                // 理论上上游正则已保证前17位为数字，这里保留防御式校验，防止未来调用链变更或绕过校验时产生静默错误。
                throw new BusinessException(400, "身份证号包含非法字符");
            }
            sum += digit * ID_CARD_WEIGHTS[i];
        }
        return ID_CARD_CHECK_CODES[sum % 11];
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
        // 清空所有用户的密码字段，并解密身份证字段（如有）以保持前端展示兼容性。
        result.getRecords().forEach(u -> {
            u.setPassword(null);
            u.setIdCard(idCardCryptoService.decryptFromStorage(u.getIdCard()));
        });
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
