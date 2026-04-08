package com.houseleasing.controller;

import com.houseleasing.common.Result;
import com.houseleasing.common.exception.BusinessException;
import com.houseleasing.entity.House;
import com.houseleasing.entity.User;
import com.houseleasing.mapper.UserMapper;
import com.houseleasing.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 推荐系统控制器
 *
 * @author hongwenhao
 * @description 提供个性化房源推荐的 REST API，基于用户行为数据进行协同过滤推荐，
 *              需要 JWT 认证
 */
@Tag(name = "Recommendation", description = "House recommendations")
@RestController
@RequestMapping("/api/recommendations") // 推荐接口统一前缀
@RequiredArgsConstructor // 自动注入依赖服务
@SecurityRequirement(name = "Bearer Authentication")
public class RecommendationController { // 处理“猜你喜欢”房源推荐请求

    private final RecommendationService recommendationService; // 推荐算法服务
    private final UserMapper userMapper; // 用户查询组件

    /**
     * 为当前登录用户获取个性化推荐房源列表
     *
     * @param userDetails 当前登录用户信息
     * @param limit       最多返回的推荐数量，默认 10 条
     * @return 推荐的房源列表
     */
    @Operation(summary = "Get recommended houses for current user")
    @GetMapping
    public Result<List<House>> getRecommendations(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "10") int limit) { // 默认最多返回 10 条推荐房源
        User user = resolveUser(userDetails.getUsername()); // 从登录信息中解析出当前用户
        return Result.success(recommendationService.getRecommendedHouses(user.getId(), limit)); // 按当前用户生成个性化推荐并返回
    }

    /**
     * 根据用户名解析用户信息
     *
     * @param username 用户名
     * @return 对应的用户实体
     */
    private User resolveUser(String username) { // 辅助方法：按用户名查询用户
        User user = userMapper.selectByUsername(username); // 从数据库读取用户资料
        if (user == null) { // 如果没有该用户
            throw new BusinessException(404, "用户不存在"); // 返回“用户不存在”错误
        }
        return user; // 返回用户实体给调用方继续使用
    }
}
