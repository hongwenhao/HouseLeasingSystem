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
@Service // 声明为用户业务服务
@RequiredArgsConstructor
public class UserServiceImpl implements UserService { // 用户主流程实现：注册登录、实名、改密、后台管理

    /** 每日首次登录可获得的信用分奖励 */
    private static final int DAILY_LOGIN_CREDIT_BONUS = 1; // 每日首次登录固定加 1 分信用分
    /** 信用分系统上限 */
    private static final int CREDIT_SCORE_MAX = 200; // 信用分上限，防止无限增长
    /** 每日首次登录提醒文案模板 */
    private static final String DAILY_LOGIN_BONUS_MESSAGE_TEMPLATE = "今日登录成功，信用分+%d（每日仅首次登录生效）"; // 首次登录消息模板
    /** 非首次登录提醒文案 */
    private static final String DAILY_LOGIN_REPEAT_MESSAGE = "登录成功（今日已完成登录加分，不重复累计）"; // 非首次登录提示文案
    /** 中国大陆身份证号基础格式：18 位，前 17 位数字 + 末位数字或 X */
    private static final String CHINA_ID_CARD_REGEX = "^\\d{17}[\\dXx]$"; // 身份证基础格式正则
    /** 身份证校验位计算权重（国家标准 GB 11643） */
    private static final int[] ID_CARD_WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2}; // 身份证前17位加权系数
    /** 校验位映射表：sum % 11 后对应字符 */
    private static final char[] ID_CARD_CHECK_CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'}; // 校验码映射表

    private final UserMapper userMapper; // 用户表访问组件
    private final PasswordEncoder passwordEncoder; // 密码加密/校验组件
    private final JwtUtil jwtUtil; // JWT 生成工具
    private final RedisTemplate<String, Object> redisTemplate; // Redis 组件（每日登录去重等）
    private final MessageProducer messageProducer; // MQ 消息发送组件
    private final IdCardCryptoService idCardCryptoService; // 身份证加解密服务

    /**
     * 用户注册：验证用户名、手机号、邮箱的唯一性，加密密码后保存用户信息
     *
     * @param request 注册请求参数（手机号和邮箱均为必填项，且不可重复）
     * @return 注册成功的用户对象（密码字段已清空）
     */
    @Override
    @Transactional
    public User register(RegisterRequest request) { // 用户注册
        // 检查用户名是否已存在
        if (userMapper.selectByUsername(request.getUsername()) != null) { // 依据当前状态决定后续处理路径
            throw new BusinessException("用户名已存在"); // 抛出业务异常并中断当前流程
        }
        // 检查手机号唯一性（手机号为必填字段，由 DTO 层 @NotBlank 保证非空）
        if (userMapper.selectByPhone(request.getPhone()) != null) { // 在该判断成立时执行对应逻辑
            throw new BusinessException("手机号已被注册"); // 抛出业务异常并中断当前流程
        }
        if (!StringUtils.hasText(request.getEmail())) { // 在该判断成立时执行对应逻辑
            throw new BusinessException("邮箱不能为空"); // 抛出业务异常并中断当前流程
        }
        if (userMapper.selectByEmail(request.getEmail()) != null) { // 在该判断成立时执行对应逻辑
            throw new BusinessException("邮箱已被注册"); // 以异常形式提示调用方当前问题
        }
        User user = new User(); // 实例化新对象用于后续操作
        user.setUsername(request.getUsername()); // 补齐对象属性供后续流程使用
        user.setPhone(request.getPhone()); // 设置业务字段以形成完整数据
        user.setEmail(request.getEmail()); // 补齐对象属性供后续流程使用
        // 使用 BCrypt 加密存储密码
        user.setPassword(passwordEncoder.encode(request.getPassword())); // 补齐对象属性供后续流程使用
        user.setRole(request.getRole() != null ? request.getRole() : "TENANT"); // 给对象写入当前步骤需要的字段值
        user.setCreditScore(100); // 新用户初始信用分 100
        user.setIsRealNameAuth(false); // 设置业务字段以形成完整数据
        user.setStatus("ACTIVE"); // 把变更结果同步到数据库
        user.setCreateTime(LocalDateTime.now()); // 设置业务字段以形成完整数据
        user.setUpdateTime(LocalDateTime.now()); // 持久化本次状态更新
        userMapper.insert(user); // 落库保存本次新增记录
        user.setPassword(null); // 清空密码字段后返回
        return user; // 输出本方法最终结果
    }

    /**
     * 用户登录：验证账号密码，检查账号状态，生成 JWT Token
     *
     * @param request 登录请求参数
     * @return 包含 token 和用户信息的 Map
     */
    @Override
    @Transactional
    public Map<String, Object> login(LoginRequest request) { // 用户登录并返回 token
        // 支持「用户名」或「手机号」作为统一登录入口，减少前端区分字段的负担
        User user = userMapper.selectByUsernameOrPhone(request.getUsername()); // 先查出目标记录再做业务判断
        if (user == null) { // 在该判断成立时执行对应逻辑
            throw new BusinessException(401, "用户不存在，请确定用户名或手机号是否正确"); // 立即返回错误避免继续执行
        }
        // 验证密码是否匹配
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) { // 依据当前状态决定后续处理路径
            throw new BusinessException(401, "密码错误，请重新输入"); // 抛出业务异常并中断当前流程
        }
        // 检查账号是否被封禁
        if ("BANNED".equals(user.getStatus())) { // 按该条件分支处理不同业务场景
            throw new BusinessException(403, "账号已被禁用"); // 立即返回错误避免继续执行
        }
        // 每日登录信用分 +1（同一自然日仅加一次）：
        // 使用 Redis SETNX + 日期维度 key 去重，key 过期时间固定 1 天。
        String loginCreditKey = "credit:login:" + user.getId() + ":" + LocalDate.now(); // 借助已有方法完成该业务动作
        Boolean firstLoginToday = redisTemplate.opsForValue().setIfAbsent(loginCreditKey, "1", 1, TimeUnit.DAYS); // 补齐对象属性供后续流程使用
        if (Boolean.TRUE.equals(firstLoginToday)) { // 依据当前状态决定后续处理路径
            int currentScore = user.getCreditScore() == null ? 0 : user.getCreditScore(); // 借助已有方法完成该业务动作
            user.setCreditScore(Math.min(CREDIT_SCORE_MAX, currentScore + DAILY_LOGIN_CREDIT_BONUS)); // 补齐对象属性供后续流程使用
            user.setUpdateTime(LocalDateTime.now()); // 持久化本次状态更新
            userMapper.updateById(user); // 执行对应服务/DAO方法推进流程
            // 每日首次登录时，向消息中心推送一条“登录成功”通知：
            // 1) 明确告知用户本次登录已记录；
            // 2) 同步提示“每日登录加分”规则，帮助用户理解信用分变动来源；
            // 3) 仅首次登录发送，避免同一天内重复提醒造成打扰。
            messageProducer.sendLoginNotification(user.getId(),
                    String.format(DAILY_LOGIN_BONUS_MESSAGE_TEMPLATE, DAILY_LOGIN_CREDIT_BONUS)); // 调用组件能力完成当前步骤
        } else { // 这里执行当前语句的核心处理
            // 非每日首次登录也保留登录通知，便于用户在消息中心追溯关键登录行为；
            // 文案明确“已登录但不重复加分”，避免用户误解积分规则。
            messageProducer.sendLoginNotification(user.getId(), DAILY_LOGIN_REPEAT_MESSAGE); // 执行对应服务/DAO方法推进流程
        }
        // 生成 JWT Token：
        // 1) Subject 使用稳定 userId，避免“用户名修改后旧 Token 立即失效”；
        // 2) Claims 保留当前用户名与角色，便于审计与问题排查。
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole()); // 借助已有方法完成该业务动作
        user.setPassword(null); // 清空密码后放入返回结果
        Map<String, Object> result = new HashMap<>(); // 创建对象承载本步骤数据
        result.put("token", token); // 执行对应服务/DAO方法推进流程
        result.put("user", user); // 借助已有方法完成该业务动作
        return result; // 返回当前阶段的处理结果
    }

    /**
     * 根据 ID 查询用户信息
     *
     * @param id 用户 ID
     * @return 用户对象（密码字段已清空）
     */
    @Override
    public User getUserById(Long id) { // 查询用户详情（含敏感字段处理）
        User user = userMapper.selectById(id); // 读取当前业务所需数据
        if (user == null) { // 在该判断成立时执行对应逻辑
            throw new BusinessException(404, "用户不存在"); // 立即返回错误避免继续执行
        }
        user.setPassword(null); // 设置业务字段以形成完整数据
        // 对外返回用户信息时按需解密身份证字段，确保前端表单“已认证信息回显”不受存储加密影响。
        user.setIdCard(idCardCryptoService.decryptFromStorage(user.getIdCard())); // 补齐对象属性供后续流程使用
        return user; // 输出本方法最终结果
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
    public User updateProfile(Long userId, UserUpdateRequest request) { // 更新用户个人资料
        User user = userMapper.selectById(userId); // 先查出目标记录再做业务判断
        if (user == null) { // 在该判断成立时执行对应逻辑
            throw new BusinessException(404, "用户不存在"); // 立即返回错误避免继续执行
        }
        // 仅更新非空字段（支持部分更新）
        if (StringUtils.hasText(request.getPhone())) { // 依据当前状态决定后续处理路径
            User existingByPhone = userMapper.selectByPhone(request.getPhone()); // 先查出目标记录再做业务判断
            if (existingByPhone != null && !existingByPhone.getId().equals(userId)) { // 在该判断成立时执行对应逻辑
                throw new BusinessException("手机号已被注册"); // 抛出业务异常并中断当前流程
            }
            user.setPhone(request.getPhone()); // 设置业务字段以形成完整数据
        }
        if (StringUtils.hasText(request.getEmail())) { // 依据当前状态决定后续处理路径
            User existingByEmail = userMapper.selectByEmail(request.getEmail()); // 从数据库加载后续处理对象
            if (existingByEmail != null && !existingByEmail.getId().equals(userId)) { // 依据当前状态决定后续处理路径
                throw new BusinessException("邮箱已被注册"); // 以异常形式提示调用方当前问题
            }
            user.setEmail(request.getEmail()); // 补齐对象属性供后续流程使用
        }
        if (StringUtils.hasText(request.getAvatar())) user.setAvatar(request.getAvatar()); // 依据当前状态决定后续处理路径
        if (StringUtils.hasText(request.getUsername())) { // 在该判断成立时执行对应逻辑
            User existingByUsername = userMapper.selectByUsername(request.getUsername()); // 从数据库加载后续处理对象
            if (existingByUsername != null && !existingByUsername.getId().equals(userId)) { // 在该判断成立时执行对应逻辑
                throw new BusinessException("用户名已存在"); // 抛出业务异常并中断当前流程
            }
            user.setUsername(request.getUsername()); // 补齐对象属性供后续流程使用
        }
        if (request.getGender() != null) user.setGender(request.getGender()); // 依据当前状态决定后续处理路径
        user.setUpdateTime(LocalDateTime.now()); // 持久化本次状态更新
        try { // 在当前步骤完成必要业务动作
            userMapper.updateById(user); // 执行对应服务/DAO方法推进流程
        } catch (DuplicateKeyException e) { // 在当前步骤完成必要业务动作
            String message = e.getMessage(); // 执行对应服务/DAO方法推进流程
            if (message != null) { // 在该判断成立时执行对应逻辑
                if (message.contains("users.username")) { // 按该条件分支处理不同业务场景
                    throw new BusinessException("用户名已存在"); // 抛出业务异常并中断当前流程
                }
                if (message.contains("users.phone")) { // 依据当前状态决定后续处理路径
                    throw new BusinessException("手机号已被注册"); // 抛出业务异常并中断当前流程
                }
                if (message.contains("users.email")) { // 在该判断成立时执行对应逻辑
                    throw new BusinessException("邮箱已被注册"); // 以异常形式提示调用方当前问题
                }
            }
            throw new BusinessException("资料更新失败，存在重复数据"); // 立即返回错误避免继续执行
        }
        user.setPassword(null); // 设置业务字段以形成完整数据
        // 更新资料接口复用用户对象直接返回，需要解密身份证以保持接口输出语义稳定。
        user.setIdCard(idCardCryptoService.decryptFromStorage(user.getIdCard())); // 补齐对象属性供后续流程使用
        return user; // 输出本方法最终结果
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
    public void realNameAuth(Long userId, String realName, String idCard) { // 提交实名认证
        User user = userMapper.selectById(userId); // 先查出目标记录再做业务判断
        if (user == null) { // 在该判断成立时执行对应逻辑
            throw new BusinessException(404, "用户不存在"); // 立即返回错误避免继续执行
        }
        if (!StringUtils.hasText(realName) || !StringUtils.hasText(idCard)) { // 在该判断成立时执行对应逻辑
            throw new BusinessException(400, "真实姓名和身份证号不能为空"); // 立即返回错误避免继续执行
        }
        // 入参统一去空白，避免用户输入前后空格导致“看起来一样但校验失败/存储不一致”。
        String normalizedRealName = realName.trim(); // 调用组件能力完成当前步骤
        String normalizedIdCard = idCard.trim().toUpperCase(); // 执行对应服务/DAO方法推进流程
        // 实名认证时强制身份证号校验，防止无效证件号写入系统影响合同与风控流程。
        validateChineseIdCard(normalizedIdCard); // 执行对应服务/DAO方法推进流程

        user.setRealName(normalizedRealName); // 补齐对象属性供后续流程使用
        // 身份证号入库前做对称加密，避免敏感明文直接落库。
        user.setIdCard(idCardCryptoService.encryptForStorage(normalizedIdCard)); // 设置业务字段以形成完整数据
        user.setIsRealNameAuth(true); // 标记实名认证完成
        user.setUpdateTime(LocalDateTime.now()); // 持久化本次状态更新
        userMapper.updateById(user); // 执行对应服务/DAO方法推进流程
    }

    /**
     * 校验中国大陆身份证号：
     * 1) 基础正则：18 位，前 17 位数字 + 最后一位数字/X；
     * 2) 出生日期段有效性：yyyyMMdd 必须是合法自然日；
     * 3) 校验位正确性：按 GB 11643 算法验证第 18 位。
     * 不通过时抛出统一业务异常，便于前端提示用户修正输入。
     */
    private void validateChineseIdCard(String idCard) { // 调用组件能力完成当前步骤
        if (!idCard.matches(CHINA_ID_CARD_REGEX)) { // 在该判断成立时执行对应逻辑
            throw new BusinessException(400, "身份证号格式不正确，请输入18位身份证号"); // 抛出业务异常并中断当前流程
        }
        String birthText = idCard.substring(6, 14); // 借助已有方法完成该业务动作
        if (!isValidBirthDate(birthText)) { // 按该条件分支处理不同业务场景
            throw new BusinessException(400, "身份证号中的出生日期无效"); // 立即返回错误避免继续执行
        }
        char expectedCheckCode = calculateIdCardCheckCode(idCard.substring(0, 17)); // 借助已有方法完成该业务动作
        if (idCard.charAt(17) != expectedCheckCode) { // 依据当前状态决定后续处理路径
            throw new BusinessException(400, "身份证号校验位不正确"); // 抛出业务异常并中断当前流程
        }
    }

    /**
     * 身份证出生日期段校验：
     * - 仅接受 8 位数字 yyyyMMdd；
     * - 通过 LocalDate.of 做严格日期校验（含闰年规则）；
     * - 非法日期返回 false，不抛异常，交由上层统一抛业务异常。
     */
    private boolean isValidBirthDate(String birthText) { // 调用组件能力完成当前步骤
        if (!birthText.matches("\\d{8}")) { // 在该判断成立时执行对应逻辑
            return false; // 把结果交还给上层调用方
        }
        try { // 在当前步骤完成必要业务动作
            int year = Integer.parseInt(birthText.substring(0, 4)); // 执行对应服务/DAO方法推进流程
            int month = Integer.parseInt(birthText.substring(4, 6)); // 借助已有方法完成该业务动作
            int day = Integer.parseInt(birthText.substring(6, 8)); // 借助已有方法完成该业务动作
            LocalDate.of(year, month, day); // 执行对应服务/DAO方法推进流程
            return true; // 输出本方法最终结果
        } catch (RuntimeException ex) { // 这里执行当前语句的核心处理
            return false; // 把结果交还给上层调用方
        }
    }

    /**
     * 依据身份证前 17 位计算校验位（GB 11643）：
     * 1) 每一位数字与固定权重相乘并求和；
     * 2) 对 11 取模；
     * 3) 用映射表得到最终校验位字符。
     */
    private char calculateIdCardCheckCode(String base17) { // 借助已有方法完成该业务动作
        int sum = 0; // 在当前步骤完成必要业务动作
        for (int i = 0; i < base17.length(); i++) { // 逐条执行同类处理逻辑
            int digit = Character.digit(base17.charAt(i), 10); // 调用组件能力完成当前步骤
            if (digit < 0) { // 依据当前状态决定后续处理路径
                // 理论上上游正则已保证前17位为数字，这里保留防御式校验，防止未来调用链变更或绕过校验时产生静默错误。
                throw new BusinessException(400, "身份证号包含非法字符"); // 以异常形式提示调用方当前问题
            }
            sum += digit * ID_CARD_WEIGHTS[i]; // 在当前步骤完成必要业务动作
        }
        return ID_CARD_CHECK_CODES[sum % 11]; // 返回当前阶段的处理结果
    }

    /**
     * 修改用户密码：验证旧密码后使用 BCrypt 加密新密码并更新
     *
     * @param userId  当前登录用户 ID
     * @param request 包含旧密码和新密码的请求对象
     */
    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) { // 修改登录密码
        User user = userMapper.selectById(userId); // 先查出目标记录再做业务判断
        if (user == null) { // 在该判断成立时执行对应逻辑
            throw new BusinessException(404, "用户不存在"); // 立即返回错误避免继续执行
        }
        // 验证旧密码是否正确
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) { // 按该条件分支处理不同业务场景
            throw new BusinessException(400, "旧密码不正确"); // 抛出业务异常并中断当前流程
        }
        // 使用 BCrypt 加密新密码并更新
        user.setPassword(passwordEncoder.encode(request.getNewPassword())); // 给对象写入当前步骤需要的字段值
        user.setUpdateTime(LocalDateTime.now()); // 持久化本次状态更新
        userMapper.updateById(user); // 执行对应服务/DAO方法推进流程
    }

    /**
     * 重置用户密码（忘记密码功能）
     * 通过用户名和手机号验证用户身份，验证通过后使用 BCrypt 加密新密码并更新
     *
     * @param request 包含用户名、手机号和新密码的请求对象
     */
    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) { // 忘记密码重置
        User user = userMapper.selectByUsername(request.getUsername()); // 从数据库加载后续处理对象
        // 使用统一的错误信息，防止用户名枚举攻击
        if (user == null || user.getPhone() == null || !user.getPhone().equals(request.getPhone())) { // 在该判断成立时执行对应逻辑
            throw new BusinessException(400, "用户名或手机号不匹配"); // 以异常形式提示调用方当前问题
        }
        // 使用 BCrypt 加密新密码并更新
        user.setPassword(passwordEncoder.encode(request.getNewPassword())); // 给对象写入当前步骤需要的字段值
        user.setUpdateTime(LocalDateTime.now()); // 持久化本次状态更新
        userMapper.updateById(user); // 执行对应服务/DAO方法推进流程
    }

    /**
     * 更新用户信用评分，分值自动限制在 0-200 范围内
     *
     * @param userId 用户 ID
     * @param delta  评分变动值（正数加分，负数减分）
     */
    @Override
    @Transactional
    public void updateCreditScore(Long userId, int delta) { // 调整用户信用分
        User user = userMapper.selectById(userId); // 先查出目标记录再做业务判断
        if (user == null) { // 在该判断成立时执行对应逻辑
            throw new BusinessException(404, "用户不存在"); // 立即返回错误避免继续执行
        }
        // 计算新评分并限制在合法范围 [0, 200]
        int newScore = Math.min(200, Math.max(0, user.getCreditScore() + delta)); // 借助已有方法完成该业务动作
        user.setCreditScore(newScore); // 补齐对象属性供后续流程使用
        user.setUpdateTime(LocalDateTime.now()); // 持久化本次状态更新
        userMapper.updateById(user); // 执行对应服务/DAO方法推进流程
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
    public PageResult<User> listUsers(int page, int size, String keyword) { // 管理员分页查询用户
        Page<User> pageObj = new Page<>(page, size); // 创建对象承载本步骤数据
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>(); // 从数据库加载后续处理对象
        if (StringUtils.hasText(keyword)) { // 依据当前状态决定后续处理路径
            // 支持按用户名、手机号或邮箱进行模糊搜索
            wrapper.like(User::getUsername, keyword)
                    .or().like(User::getPhone, keyword)
                    .or().like(User::getEmail, keyword); // 调用组件能力完成当前步骤
        }
        wrapper.orderByDesc(User::getCreateTime); // 借助已有方法完成该业务动作
        Page<User> result = userMapper.selectPage(pageObj, wrapper); // 读取当前业务所需数据
        // 清空所有用户的密码字段，并解密身份证字段（如有）以保持前端展示兼容性。
        result.getRecords().forEach(u -> { // 调用组件能力完成当前步骤
            u.setPassword(null); // 补齐对象属性供后续流程使用
            u.setIdCard(idCardCryptoService.decryptFromStorage(u.getIdCard())); // 设置业务字段以形成完整数据
        }); // 按既定流程继续处理后续逻辑
        return PageResult.of(result.getTotal(), result.getRecords(), page, size); // 输出本方法最终结果
    }

    /**
     * 封禁用户账号，设置状态为 BANNED
     *
     * @param userId 要封禁的用户 ID
     */
    @Override
    @Transactional
    public void banUser(Long userId) { // 封禁用户
        User user = userMapper.selectById(userId); // 先查出目标记录再做业务判断
        if (user == null) { // 在该判断成立时执行对应逻辑
            throw new BusinessException(404, "用户不存在"); // 立即返回错误避免继续执行
        }
        user.setStatus("BANNED"); // 持久化本次状态更新
        user.setUpdateTime(LocalDateTime.now()); // 持久化本次状态更新
        userMapper.updateById(user); // 执行对应服务/DAO方法推进流程
    }

    /**
     * 解封用户账号，恢复状态为 ACTIVE
     *
     * @param userId 要解封的用户 ID
     */
    @Override
    @Transactional
    public void unbanUser(Long userId) { // 解封用户
        User user = userMapper.selectById(userId); // 先查出目标记录再做业务判断
        if (user == null) { // 在该判断成立时执行对应逻辑
            throw new BusinessException(404, "用户不存在"); // 立即返回错误避免继续执行
        }
        user.setStatus("ACTIVE"); // 把变更结果同步到数据库
        user.setUpdateTime(LocalDateTime.now()); // 持久化本次状态更新
        userMapper.updateById(user); // 执行对应服务/DAO方法推进流程
    }
}
