package com.houseleasing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.houseleasing.entity.House;
import com.houseleasing.mapper.HouseMapper;
import com.houseleasing.mapper.UserBehaviorMapper;
import com.houseleasing.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final UserBehaviorMapper userBehaviorMapper;
    private final HouseMapper houseMapper;

    @Override
    public List<House> getRecommendedHouses(Long userId, int limit) {
        try {
            // Step 1: Get houses the user has interacted with
            List<Long> userHouseIds = userBehaviorMapper.selectHouseIdsByUserId(userId);
            Set<Long> userHouseSet = new HashSet<>(userHouseIds);

            // Step 2: Find similar users who interacted with same houses
            Set<Long> similarUsers = new HashSet<>();
            for (Long houseId : userHouseIds) {
                List<Long> otherUsers = userBehaviorMapper.selectUserIdsByHouseId(houseId, userId);
                similarUsers.addAll(otherUsers);
            }

            // Step 3: Get houses from similar users that current user hasn't seen
            Map<Long, Integer> houseFrequency = new HashMap<>();
            for (Long similarUserId : similarUsers) {
                List<Long> similarUserHouses = userBehaviorMapper.selectHouseIdsByUserId(similarUserId);
                for (Long houseId : similarUserHouses) {
                    if (!userHouseSet.contains(houseId)) {
                        houseFrequency.merge(houseId, 1, Integer::sum);
                    }
                }
            }

            // Step 4: Sort by frequency and fetch houses
            List<Long> recommendedIds = houseFrequency.entrySet().stream()
                    .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                    .limit(limit)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            List<House> recommended = new ArrayList<>();
            if (!recommendedIds.isEmpty()) {
                LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>();
                wrapper.in(House::getId, recommendedIds)
                        .eq(House::getStatus, "ONLINE");
                recommended = houseMapper.selectList(wrapper);
            }

            // Step 5: Pad with popular houses if not enough
            if (recommended.size() < limit) {
                LambdaQueryWrapper<House> popularWrapper = new LambdaQueryWrapper<>();
                popularWrapper.eq(House::getStatus, "ONLINE");
                if (!recommended.isEmpty()) {
                    List<Long> existingIds = recommended.stream().map(House::getId).collect(Collectors.toList());
                    popularWrapper.notIn(House::getId, existingIds);
                }
                popularWrapper.orderByDesc(House::getViewCount);
                com.baomidou.mybatisplus.extension.plugins.pagination.Page<House> page =
                        new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, limit - recommended.size());
                List<House> popular = houseMapper.selectPage(page, popularWrapper).getRecords();
                recommended.addAll(popular);
            }

            return recommended;
        } catch (Exception e) {
            log.error("Recommendation failed: {}", e.getMessage());
            // Fallback: return popular houses
            LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(House::getStatus, "ONLINE").orderByDesc(House::getViewCount);
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<House> page =
                    new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, limit);
            return houseMapper.selectPage(page, wrapper).getRecords();
        }
    }
}
