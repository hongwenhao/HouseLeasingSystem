package com.houseleasing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.houseleasing.activiti.WorkflowService;
import com.houseleasing.common.PageResult;
import com.houseleasing.common.exception.BusinessException;
import com.houseleasing.dto.HouseSearchRequest;
import com.houseleasing.entity.House;
import com.houseleasing.entity.User;
import com.houseleasing.entity.UserBehavior;
import com.houseleasing.mapper.HouseMapper;
import com.houseleasing.mapper.UserBehaviorMapper;
import com.houseleasing.mapper.UserMapper;
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

/**
 * 房源服务实现类
 *
 * @author HouseLeasingSystem开发团队
 * @description 实现房源相关的所有业务逻辑，包括房源的发布、更新、搜索、审核、
 * 收藏和浏览量统计，热门房源使用 Redis 缓存提高查询性能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HouseServiceImpl implements HouseService {

    private final WorkflowService workflowService;
    private final HouseMapper houseMapper;
    private final UserBehaviorMapper userBehaviorMapper;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 发布新房源，设置初始状态为待审核，清除热门房源缓存
     *
     * @param house   房源信息
     * @param ownerId 房东用户 ID
     * @return 创建成功的房源对象
     */
    @Override
    @Transactional
    @CacheEvict(value = "hotHouses", allEntries = true) // 发布新房源后清除热门房源缓存
    public House addHouse(House house, Long ownerId) {
        house.setOwnerId(ownerId);
        house.setStatus("PENDING"); // 新房源默认状态为待审核
        house.setViewCount(0);
        house.setCreateTime(LocalDateTime.now());
        house.setUpdateTime(LocalDateTime.now());
        houseMapper.insert(house);
        // 启动房源审核流程并写回流程实例 ID，流程启动失败将导致事务回滚
        try {
            String processInstanceId = workflowService.startHouseApprovalProcess(house.getId(), ownerId);
            house.setWorkflowInstanceId(processInstanceId);
            houseMapper.updateById(house);
        } catch (Exception ex) {
            log.error("Failed to start house approval workflow for house {}", house.getId(), ex);
            throw new BusinessException("房源审核流程启动失败，流程服务不可用");
        }
        return house;
    }

    /**
     * 更新房源信息，验证操作人是否为该房源的房东
     *
     * @param id      房源 ID
     * @param house   更新内容
     * @param ownerId 操作人用户 ID
     * @return 更新后的房源对象
     */
    @Override
    @Transactional
    @CacheEvict(value = "hotHouses", allEntries = true) // 更新房源后清除热门房源缓存
    public House updateHouse(Long id, House house, Long ownerId) {
        House existing = houseMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "房源不存在");
        }
        // 验证操作人是否是该房源的所有者
        if (!existing.getOwnerId().equals(ownerId)) {
            throw new BusinessException(403, "没有权限修改该房源");
        }
        house.setId(id);
        house.setOwnerId(ownerId);
        house.setUpdateTime(LocalDateTime.now());
        houseMapper.updateById(house);
        return houseMapper.selectById(id);
    }

    /**
     * 查询房源详情，同时尝试增加浏览量（失败不影响主流程）
     *
     * @param id 房源 ID
     * @return 房源详情对象
     */
    @Override
    public House getHouseById(Long id) {
        House house = houseMapper.selectById(id);
        if (house == null) {
            throw new BusinessException(404, "房源不存在");
        }
        // 尝试增加浏览量，失败时只打印警告不影响正常查询
        try {
            houseMapper.incrementViewCount(id);
        } catch (Exception e) {
            log.warn("Failed to increment view count: {}", e.getMessage());
        }
        // 关联填充房东信息（隐去密码等敏感字段）
        if (house.getOwnerId() != null) {
            User owner = userMapper.selectById(house.getOwnerId());
            if (owner != null) {
                User sanitized = new User();
                sanitized.setId(owner.getId());
                sanitized.setUsername(owner.getUsername());
                sanitized.setPhone(owner.getPhone());
                sanitized.setAvatar(owner.getAvatar());
                sanitized.setRealName(owner.getRealName());
                sanitized.setCreditScore(owner.getCreditScore());
                sanitized.setIsRealNameAuth(owner.getIsRealNameAuth());
                house.setLandlord(sanitized);
            }
        }
        return house;
    }

    /**
     * 按条件分页搜索房源，主查询失败时降级为简单查询
     *
     * @param request 搜索请求参数
     * @return 符合条件的分页房源列表
     */
    @Override
    public PageResult<House> searchHouses(HouseSearchRequest request) {
        Page<House> page = new Page<>(request.getPage(), request.getSize());
        try {
            // 使用 XML Mapper 中的复杂条件查询
            com.baomidou.mybatisplus.core.metadata.IPage<House> result = houseMapper.selectByCondition(page, request);
            return PageResult.of(result.getTotal(), result.getRecords(), request.getPage(), request.getSize());
        } catch (Exception e) {
            // 降级处理：复杂查询失败时退回简单查询
            log.error("复杂的房屋搜索出错了，系统正在改用基础搜索方式重试: {}", e.getMessage());
            LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(House::getStatus, "ONLINE");
            wrapper.orderByDesc(House::getCreateTime);
            Page<House> result = houseMapper.selectPage(page, wrapper);
            return PageResult.of(result.getTotal(), result.getRecords(), request.getPage(), request.getSize());
        }
    }

    /**
     * 管理员审核房源：通过则上线，拒绝则标记为已拒绝，并清除缓存
     *
     * @param id       房源 ID
     * @param approved 是否批准
     * @param reason   审核意见
     */
    @Override
    @Transactional
    @CacheEvict(value = "hotHouses", allEntries = true) // 审核状态变化后清除缓存
    public void approveHouse(Long id, boolean approved, String reason) {
        House house = houseMapper.selectById(id);
        if (house == null) {
            throw new BusinessException(404, "房源不存在");
        }
        house.setStatus(approved ? "ONLINE" : "REJECTED");
        house.setUpdateTime(LocalDateTime.now());
        houseMapper.updateById(house);
        if (house.getWorkflowInstanceId() != null) {
            workflowService.approveHouseProcess(house.getWorkflowInstanceId(), approved, reason);
        }
        log.info("House {} {}: {}", id, approved ? "approved" : "rejected", reason);
    }

    /**
     * 查询指定房东发布的所有房源（分页）
     *
     * @param ownerId 房东用户 ID
     * @param page    当前页码
     * @param size    每页大小
     * @return 分页房源列表
     */
    @Override
    public PageResult<House> listOwnerHouses(Long ownerId, int page, int size) {
        Page<House> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(House::getOwnerId, ownerId);
        wrapper.orderByDesc(House::getCreateTime);
        Page<House> result = houseMapper.selectPage(pageObj, wrapper);
        return PageResult.of(result.getTotal(), result.getRecords(), page, size);
    }

    /**
     * 用户收藏房源，若已收藏则忽略（幂等操作）
     *
     * @param userId  用户 ID
     * @param houseId 房源 ID
     */
    @Override
    @Transactional
    public void collectHouse(Long userId, Long houseId) {
        // 检查是否已经收藏过该房源
        LambdaQueryWrapper<UserBehavior> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBehavior::getUserId, userId)
                .eq(UserBehavior::getHouseId, houseId)
                .eq(UserBehavior::getBehaviorType, "COLLECT");
        UserBehavior existing = userBehaviorMapper.selectOne(wrapper);
        if (existing == null) {
            // 未收藏过，新增收藏行为记录
            UserBehavior behavior = new UserBehavior();
            behavior.setUserId(userId);
            behavior.setHouseId(houseId);
            behavior.setBehaviorType("COLLECT");
            behavior.setCreateTime(LocalDateTime.now());
            userBehaviorMapper.insert(behavior);
        }
    }

    /**
     * 获取热门房源列表（按浏览量降序，最多 10 条），结果缓存到 Redis
     *
     * @return 热门房源列表
     */
    @Override
    @Cacheable(value = "hotHouses", key = "'all'") // 缓存热门房源列表，key 为 'hotHouses::all'
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

    /**
     * 增加指定房源的浏览量，失败时记录日志不抛出异常
     *
     * @param houseId 房源 ID
     */
    @Override
    public void incrementViewCount(Long houseId) {
        try {
            houseMapper.incrementViewCount(houseId);
        } catch (Exception e) {
            log.warn("Failed to increment view count for house {}: {}", houseId, e.getMessage());
        }
    }
}
