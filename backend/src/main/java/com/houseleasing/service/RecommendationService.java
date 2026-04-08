package com.houseleasing.service;

import com.houseleasing.entity.House;

import java.util.List;

/**
 * 房源推荐服务接口
 *
 * @author hongwenhao
 * @description 定义房源推荐的业务操作，基于协同过滤算法为用户提供个性化推荐
 */
public interface RecommendationService { // 推荐能力抽象：根据用户行为返回个性化房源

    /**
     * 为指定用户获取推荐房源列表
     * 算法基于协同过滤：分析用户历史行为，找到相似用户，推荐相似用户感兴趣的房源
     *
     * @param userId 用户 ID
     * @param limit  最多返回的推荐房源数量
     * @return 推荐的房源列表
     */
    List<House> getRecommendedHouses(Long userId, int limit); // 查询指定用户的推荐房源列表
}
