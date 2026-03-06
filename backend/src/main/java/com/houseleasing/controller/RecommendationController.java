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

@Tag(name = "Recommendation", description = "House recommendations")
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserMapper userMapper;

    @Operation(summary = "Get recommended houses for current user")
    @GetMapping
    public Result<List<House>> getRecommendations(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "10") int limit) {
        User user = resolveUser(userDetails.getUsername());
        return Result.success(recommendationService.getRecommendedHouses(user.getId(), limit));
    }

    private User resolveUser(String username) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        return user;
    }
}
