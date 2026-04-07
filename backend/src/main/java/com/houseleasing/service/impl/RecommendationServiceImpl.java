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

/**
 * 房源推荐服务实现类
 *
 * @author HouseLeasingSystem开发团队
 * @description 基于协同过滤算法实现个性化房源推荐。
 *              算法流程：获取用户历史行为 → 找到相似用户 → 推荐相似用户感兴趣的房源 → 不足时补充热门房源
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final UserBehaviorMapper userBehaviorMapper;
    private final HouseMapper houseMapper;

    /**
     * 基于协同过滤为用户推荐房源
     * 算法分五步执行，异常时降级为热门房源推荐
     *
     * @param userId 用户 ID
     * @param limit  最多返回的推荐数量
     * @return 推荐的房源列表
     */
    @Override
    public List<House> getRecommendedHouses(Long userId, int limit) {
        try {
            // 边界保护：limit 非法时直接返回空集合，避免出现负分页参数或无意义查询
            if (limit <= 0) {
                return Collections.emptyList();
            }

            // 第一步：获取当前用户有过交互的所有房源集合
            // 交互行为由 UserBehaviorMapper 统一维护（浏览/收藏等），此处不区分行为权重
            List<Long> userHouseIds = userBehaviorMapper.selectHouseIdsByUserId(userId);
            Set<Long> userHouseSet = new HashSet<>(userHouseIds);

            // 第二步：找到与当前用户行为相似的其他用户（基于共同感兴趣的房源>=1）
            Set<Long> similarUsers = new HashSet<>();
            for (Long houseId : userHouseIds) {
                List<Long> otherUsers = userBehaviorMapper.selectUserIdsByHouseId(houseId, userId);
                similarUsers.addAll(otherUsers);
            }

            // 第三步：统计相似用户感兴趣但当前用户未看过的房源的出现频次
            Map<Long, Integer> houseFrequency = new HashMap<>();
            for (Long similarUserId : similarUsers) {
                List<Long> similarUserHouses = userBehaviorMapper.selectHouseIdsByUserId(similarUserId);
                for (Long houseId : similarUserHouses) {
                    if (!userHouseSet.contains(houseId)) {
                        // 相似用户每次出现该房源就计数 +1
                        houseFrequency.merge(houseId, 1, Integer::sum);
                    }
                }
            }

            // 第四步：按频次降序排列，取前 limit 条房源 ID
            List<Long> recommendedIds = houseFrequency.entrySet().stream()
                    .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                    .limit(limit)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            List<House> recommended = new ArrayList<>();
            if (!recommendedIds.isEmpty()) {
                // 批量查询推荐房源，只返回已上线的房源
                LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>();
                wrapper.in(House::getId, recommendedIds)
                        .eq(House::getStatus, "ONLINE");
                recommended = houseMapper.selectList(wrapper);
            }

            // 第五步：如果推荐数量不足，用热门房源补充到指定数量
            if (recommended.size() < limit) {
                LambdaQueryWrapper<House> popularWrapper = new LambdaQueryWrapper<>();
                popularWrapper.eq(House::getStatus, "ONLINE");
                if (!recommended.isEmpty()) {
                    // 排除已推荐的房源，避免重复
                    List<Long> existingIds = recommended.stream().map(House::getId).collect(Collectors.toList());
                    popularWrapper.notIn(House::getId, existingIds);
                }
                popularWrapper.orderByDesc(House::getViewCount); // 按浏览量排序
                com.baomidou.mybatisplus.extension.plugins.pagination.Page<House> page =
                        new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, limit - recommended.size());
                List<House> popular = houseMapper.selectPage(page, popularWrapper).getRecords();
                // 使用 addAll 追加热门结果，保持“个性化推荐在前、热门兜底在后”的展示顺序
                recommended.addAll(popular);
            }

            return recommended;
        } catch (Exception e) {
            log.error("Recommendation failed: {}", e.getMessage());
            // 异常降级：推荐失败时直接返回热门房源，保障接口可用性
            LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(House::getStatus, "ONLINE").orderByDesc(House::getViewCount);
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<House> page =
                    new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, limit);
            return houseMapper.selectPage(page, wrapper).getRecords();
        }
    }
}
