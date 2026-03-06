package com.houseleasing.service;

import com.houseleasing.common.PageResult;
import com.houseleasing.dto.LoginRequest;
import com.houseleasing.dto.RegisterRequest;
import com.houseleasing.dto.UserUpdateRequest;
import com.houseleasing.entity.User;

import java.util.Map;

public interface UserService {
    User register(RegisterRequest request);
    Map<String, Object> login(LoginRequest request);
    User getUserById(Long id);
    User updateProfile(Long userId, UserUpdateRequest request);
    void realNameAuth(Long userId, String realName, String idCard);
    void updateCreditScore(Long userId, int delta);
    PageResult<User> listUsers(int page, int size, String keyword);
    void banUser(Long userId);
    void unbanUser(Long userId);
}
