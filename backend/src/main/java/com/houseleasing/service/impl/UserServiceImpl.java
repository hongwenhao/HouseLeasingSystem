package com.houseleasing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.houseleasing.common.PageResult;
import com.houseleasing.common.exception.BusinessException;
import com.houseleasing.common.utils.JwtUtil;
import com.houseleasing.dto.LoginRequest;
import com.houseleasing.dto.RegisterRequest;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public User register(RegisterRequest request) {
        if (userMapper.selectByUsername(request.getUsername()) != null) {
            throw new BusinessException("Username already exists");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? request.getRole() : "TENANT");
        user.setCreditScore(100);
        user.setIsRealNameAuth(false);
        user.setStatus("ACTIVE");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        user.setPassword(null);
        return user;
    }

    @Override
    public Map<String, Object> login(LoginRequest request) {
        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException(401, "User not found");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "Invalid password");
        }
        if ("BANNED".equals(user.getStatus())) {
            throw new BusinessException(403, "Account is banned");
        }
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        user.setPassword(null);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);
        return result;
    }

    @Override
    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        user.setPassword(null);
        return user;
    }

    @Override
    @Transactional
    public User updateProfile(Long userId, UserUpdateRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        if (StringUtils.hasText(request.getPhone())) user.setPhone(request.getPhone());
        if (StringUtils.hasText(request.getEmail())) user.setEmail(request.getEmail());
        if (StringUtils.hasText(request.getAvatar())) user.setAvatar(request.getAvatar());
        if (StringUtils.hasText(request.getUsername())) user.setUsername(request.getUsername());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        user.setPassword(null);
        return user;
    }

    @Override
    @Transactional
    public void realNameAuth(Long userId, String realName, String idCard) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        user.setRealName(realName);
        user.setIdCard(idCard);
        user.setIsRealNameAuth(true);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void updateCreditScore(Long userId, int delta) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        int newScore = Math.min(200, Math.max(0, user.getCreditScore() + delta));
        user.setCreditScore(newScore);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public PageResult<User> listUsers(int page, int size, String keyword) {
        Page<User> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(User::getUsername, keyword)
                    .or().like(User::getPhone, keyword)
                    .or().like(User::getEmail, keyword);
        }
        wrapper.orderByDesc(User::getCreateTime);
        Page<User> result = userMapper.selectPage(pageObj, wrapper);
        result.getRecords().forEach(u -> u.setPassword(null));
        return PageResult.of(result.getTotal(), result.getRecords(), page, size);
    }

    @Override
    @Transactional
    public void banUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        user.setStatus("BANNED");
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void unbanUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        user.setStatus("ACTIVE");
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }
}
