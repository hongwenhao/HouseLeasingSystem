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
 * @author hongwenhao
 * @description 基于协同过滤算法实现个性化房源推荐。
 *              算法流程：获取用户历史行为 → 找到相似用户 → 推荐相似用户感兴趣的房源 → 不足时补充热门房源
 */
@Slf4j
@Service // 声明为推荐业务服务
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService { // 协同过滤推荐算法的实现类

    private final UserBehaviorMapper userBehaviorMapper; // 用户行为查询组件（浏览/收藏/下单）
    private final HouseMapper houseMapper; // 房源数据访问组件

    /**
     * 基于协同过滤为用户推荐房源
     * 算法分五步执行，异常时降级为热门房源推荐
     *
     * @param userId 用户 ID
     * @param limit  最多返回的推荐数量
     * @return 推荐的房源列表
     */
    @Override
    public List<House> getRecommendedHouses(Long userId, int limit) { // 给指定用户生成推荐房源列表
        try { // 主流程：协同过滤计算个性化推荐
            // 第一步：获取当前用户有过交互的所有房源集合
            // 交互行为由 UserBehaviorMapper 统一维护（浏览/收藏等），此处不区分行为权重
            List<Long> userHouseIds = userBehaviorMapper.selectHouseIdsByUserId(userId); // 查询当前用户历史交互过的房源ID
            Set<Long> userHouseSet = new HashSet<>(userHouseIds); // 转成 Set 便于后续 O(1) 排重判断

            // 第二步：找到与当前用户行为相似的其他用户（基于共同感兴趣的房源>=1）
            Set<Long> similarUsers = new HashSet<>(); // 收集“与当前用户至少有一个共同房源交互”的用户
            for (Long houseId : userHouseIds) {
                List<Long> otherUsers = userBehaviorMapper.selectUserIdsByHouseId(houseId, userId); // 找到同样关注该房源的其他用户
                similarUsers.addAll(otherUsers); // 合并进相似用户集合，自动去重
            }

            // 第三步：统计相似用户感兴趣但当前用户未看过的房源的出现频次
            Map<Long, Integer> houseFrequency = new HashMap<>(); // 统计候选房源被相似用户“共同选择”的次数
            for (Long similarUserId : similarUsers) {
                List<Long> similarUserHouses = userBehaviorMapper.selectHouseIdsByUserId(similarUserId); // 拉取每个相似用户的房源交互列表
                for (Long houseId : similarUserHouses) {
                    if (!userHouseSet.contains(houseId)) { // 过滤掉当前用户已经看过/点过的房源
                        // 相似用户每次出现该房源就计数 +1
                        houseFrequency.merge(houseId, 1, Integer::sum); // 出现一次计一次，用频次代表推荐强度
                    }
                }
            }

            // 第四步：按频次降序排列，取前 limit 条房源 ID
            List<Long> recommendedIds = houseFrequency.entrySet().stream() // 把频次 Map 转为可排序列表
                    .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                    .limit(limit)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList()); // 仅保留前 limit 个房源ID

            List<House> recommended = new ArrayList<>(); // 存放最终推荐结果
            if (!recommendedIds.isEmpty()) {
                // 批量查询推荐房源，只返回已上线的房源
                LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>(); // 构建“候选ID + 上线状态”查询
                wrapper.in(House::getId, recommendedIds)
                        .eq(House::getStatus, "ONLINE");
                recommended = houseMapper.selectList(wrapper); // 把候选ID还原为完整房源对象
            }

            // 第五步：如果推荐数量不足，用热门房源补充到指定数量
            if (recommended.size() < limit) {
                LambdaQueryWrapper<House> popularWrapper = new LambdaQueryWrapper<>(); // 构建热门房源兜底查询条件
                popularWrapper.eq(House::getStatus, "ONLINE"); // 兜底也只返回上架房源
                if (!recommended.isEmpty()) {
                    // 排除已推荐的房源，避免重复
                    List<Long> existingIds = recommended.stream().map(House::getId).collect(Collectors.toList()); // 收集已在推荐列表中的房源ID
                    popularWrapper.notIn(House::getId, existingIds); // 兜底查询排除这些ID，防止重复展示
                }
                popularWrapper.orderByDesc(House::getViewCount); // 按浏览量排序
                com.baomidou.mybatisplus.extension.plugins.pagination.Page<House> page =
                        new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, limit - recommended.size()); // 只查询“还差多少条”
                List<House> popular = houseMapper.selectPage(page, popularWrapper).getRecords(); // 取热门房源列表作为兜底补齐
                // 使用 addAll 追加热门结果，保持“个性化推荐在前、热门兜底在后”的展示顺序
                recommended.addAll(popular); // 追加到尾部，不打乱前面个性化结果顺序
            }

            return recommended; // 返回最终推荐结果（个性化 + 热门兜底）
        } catch (Exception e) {
            log.error("推荐计算失败：{}", e.getMessage()); // 记录异常原因，便于排查算法或数据问题
            // 异常降级：推荐失败时直接返回热门房源，保障接口可用性
            LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>(); // 构建热门房源查询条件
            wrapper.eq(House::getStatus, "ONLINE").orderByDesc(House::getViewCount); // 仅查上架房源并按热度倒序
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<House> page =
                    new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, limit); // 降级场景直接取前 limit 条
            return houseMapper.selectPage(page, wrapper).getRecords(); // 返回热门房源，保证接口不空返回
        }
    }
}
