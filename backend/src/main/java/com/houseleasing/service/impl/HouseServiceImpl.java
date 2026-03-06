package com.houseleasing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.houseleasing.common.PageResult;
import com.houseleasing.common.exception.BusinessException;
import com.houseleasing.dto.HouseSearchRequest;
import com.houseleasing.entity.House;
import com.houseleasing.entity.UserBehavior;
import com.houseleasing.mapper.HouseMapper;
import com.houseleasing.mapper.UserBehaviorMapper;
import com.houseleasing.service.HouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HouseServiceImpl implements HouseService {

    private final HouseMapper houseMapper;
    private final UserBehaviorMapper userBehaviorMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    @CacheEvict(value = "hotHouses", allEntries = true)
    public House addHouse(House house, Long ownerId) {
        house.setOwnerId(ownerId);
        house.setStatus("PENDING");
        house.setViewCount(0);
        house.setCreateTime(LocalDateTime.now());
        house.setUpdateTime(LocalDateTime.now());
        houseMapper.insert(house);
        return house;
    }

    @Override
    @Transactional
    @CacheEvict(value = "hotHouses", allEntries = true)
    public House updateHouse(Long id, House house, Long ownerId) {
        House existing = houseMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "House not found");
        }
        if (!existing.getOwnerId().equals(ownerId)) {
            throw new BusinessException(403, "Not authorized to update this house");
        }
        house.setId(id);
        house.setOwnerId(ownerId);
        house.setUpdateTime(LocalDateTime.now());
        houseMapper.updateById(house);
        return houseMapper.selectById(id);
    }

    @Override
    public House getHouseById(Long id) {
        House house = houseMapper.selectById(id);
        if (house == null) {
            throw new BusinessException(404, "House not found");
        }
        try {
            houseMapper.incrementViewCount(id);
        } catch (Exception e) {
            log.warn("Failed to increment view count: {}", e.getMessage());
        }
        return house;
    }

    @Override
    public PageResult<House> searchHouses(HouseSearchRequest request) {
        Page<House> page = new Page<>(request.getPage(), request.getSize());
        try {
            com.baomidou.mybatisplus.core.metadata.IPage<House> result = houseMapper.selectByCondition(page, request);
            return PageResult.of(result.getTotal(), result.getRecords(), request.getPage(), request.getSize());
        } catch (Exception e) {
            log.error("Search houses failed, falling back to simple query: {}", e.getMessage());
            LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(House::getStatus, "ONLINE");
            wrapper.orderByDesc(House::getCreateTime);
            Page<House> result = houseMapper.selectPage(page, wrapper);
            return PageResult.of(result.getTotal(), result.getRecords(), request.getPage(), request.getSize());
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "hotHouses", allEntries = true)
    public void approveHouse(Long id, boolean approved, String reason) {
        House house = houseMapper.selectById(id);
        if (house == null) {
            throw new BusinessException(404, "House not found");
        }
        house.setStatus(approved ? "ONLINE" : "REJECTED");
        house.setUpdateTime(LocalDateTime.now());
        houseMapper.updateById(house);
        log.info("House {} {}: {}", id, approved ? "approved" : "rejected", reason);
    }

    @Override
    public PageResult<House> listOwnerHouses(Long ownerId, int page, int size) {
        Page<House> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(House::getOwnerId, ownerId);
        wrapper.orderByDesc(House::getCreateTime);
        Page<House> result = houseMapper.selectPage(pageObj, wrapper);
        return PageResult.of(result.getTotal(), result.getRecords(), page, size);
    }

    @Override
    @Transactional
    public void collectHouse(Long userId, Long houseId) {
        LambdaQueryWrapper<UserBehavior> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBehavior::getUserId, userId)
                .eq(UserBehavior::getHouseId, houseId)
                .eq(UserBehavior::getBehaviorType, "COLLECT");
        UserBehavior existing = userBehaviorMapper.selectOne(wrapper);
        if (existing == null) {
            UserBehavior behavior = new UserBehavior();
            behavior.setUserId(userId);
            behavior.setHouseId(houseId);
            behavior.setBehaviorType("COLLECT");
            behavior.setCreateTime(LocalDateTime.now());
            userBehaviorMapper.insert(behavior);
        }
    }

    @Override
    @Cacheable(value = "hotHouses", key = "'all'")
    public List<House> getHotHouses() {
        try {
            LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(House::getStatus, "ONLINE");
            wrapper.orderByDesc(House::getViewCount);
            Page<House> page = new Page<>(1, 10);
            return houseMapper.selectPage(page, wrapper).getRecords();
        } catch (Exception e) {
            log.error("Failed to get hot houses: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public void incrementViewCount(Long houseId) {
        try {
            houseMapper.incrementViewCount(houseId);
        } catch (Exception e) {
            log.warn("Failed to increment view count for house {}: {}", houseId, e.getMessage());
        }
    }
}
