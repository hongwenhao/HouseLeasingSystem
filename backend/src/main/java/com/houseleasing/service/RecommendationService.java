package com.houseleasing.service;

import com.houseleasing.entity.House;

import java.util.List;

public interface RecommendationService {
    List<House> getRecommendedHouses(Long userId, int limit);
}
