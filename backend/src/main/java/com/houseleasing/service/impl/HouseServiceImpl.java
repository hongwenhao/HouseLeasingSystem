package com.houseleasing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.houseleasing.common.PageResult;
import com.houseleasing.common.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.houseleasing.dto.HouseSearchRequest;
import com.houseleasing.entity.Contract;
import com.houseleasing.entity.House;
import com.houseleasing.entity.HouseImage;
import com.houseleasing.entity.User;
import com.houseleasing.entity.UserBehavior;
import com.houseleasing.mapper.ContractMapper;
import com.houseleasing.mapper.HouseImageMapper;
import com.houseleasing.mapper.HouseMapper;
import com.houseleasing.mapper.UserBehaviorMapper;
import com.houseleasing.mapper.UserMapper;
import com.houseleasing.service.HouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 房源服务实现类
 *
 * @author hongwenhao
 * @description 实现房源相关的所有业务逻辑，包括房源的发布、更新、搜索、审核、
 * 收藏和浏览量统计，热门房源使用 Redis 缓存提高查询性能
 */
@Slf4j
@Service // 声明为房源业务服务
@RequiredArgsConstructor
public class HouseServiceImpl implements HouseService { // 房源核心业务实现：发布、查询、收藏、上下架等

    private final HouseMapper houseMapper; // 房源表访问组件
    private final ContractMapper contractMapper; // 合同表访问组件（删除/状态联动会用到）
    private final HouseImageMapper houseImageMapper; // 房源图片明细访问组件
    private final UserBehaviorMapper userBehaviorMapper; // 用户行为访问组件
    private final UserMapper userMapper; // 用户表访问组件
    private final ObjectMapper objectMapper; // JSON 解析组件
    private final RedisTemplate<String, Object> redisTemplate; // Redis 组件（缓存与计数）

    private static final String BEHAVIOR_COLLECT = "COLLECT"; // 用户“收藏房源”行为类型标识
    /**
     * 收藏行为分值（用于推荐系统行为权重）：
     * 规则约定为 VIEW=1、COLLECT=3、ORDER=5，这里固定 COLLECT=3。
     */
    private static final BigDecimal BEHAVIOR_COLLECT_SCORE = new BigDecimal("3.0");
    private static final String HOUSE_STATUS_ONLINE = "ONLINE"; // 房源上架状态
    private static final String HOUSE_STATUS_OFFLINE = "OFFLINE"; // 房源下架状态
    private static final int CREDIT_SCORE_PUBLISHING_THRESHOLD = 0; // 发布房源所需最低信用分

    /**
     * 发布新房源，设置初始状态为已上架，清除热门房源缓存
     *
     * @param house   房源信息
     * @param ownerId 房东用户 ID
     * @return 创建成功的房源对象
     */
    @Override
    @Transactional
    @CacheEvict(value = "hotHouses", allEntries = true) // 发布并立即上线房源时清除热门房源缓存
    public House addHouse(House house, Long ownerId) { // 发布新房源
        User owner = userMapper.selectById(ownerId); // 查询发布者用户信息用于权限与信用校验
        if (owner == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (owner.getCreditScore() != null && owner.getCreditScore() < CREDIT_SCORE_PUBLISHING_THRESHOLD) {
            throw new BusinessException(403, "当前信用分过低，暂不可发布房源");
        }
        house.setOwnerId(ownerId); // 绑定房源归属房东
        house.setStatus(HOUSE_STATUS_ONLINE); // 新房源默认状态为已上线
        house.setViewCount(0); // 新房源浏览量从 0 开始累计
        house.setCreateTime(LocalDateTime.now()); // 写入创建时间
        house.setUpdateTime(LocalDateTime.now()); // 初始化更新时间
        houseMapper.insert(house); // 插入房源主表记录
        // 同步写入 house_images 明细表，确保“房源主表 JSON 字段”和“图片明细表”两处数据一致
        syncHouseImages(house.getId(), house.getImages());
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
    public House updateHouse(Long id, House house, Long ownerId) { // 更新房源信息
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
        House updatedHouse = houseMapper.selectById(id);
        // 更新后按数据库最终值重建图片明细，兼容“本次请求未携带 images 字段”的场景
        syncHouseImages(id, updatedHouse != null ? updatedHouse.getImages() : null);
        return updatedHouse;
    }

    /**
     * 将 houses.images（JSON 字符串）同步到 house_images 表。
     *
     * <p>为什么需要该同步：</p>
     * <ul>
     *   <li>当前系统历史上同时保留了两种图片存储方式：主表 JSON 字段 + 明细表 house_images。</li>
     *   <li>前端发布房源时会写入 houses.images，但若不额外同步，house_images 会一直为空。</li>
     *   <li>这里采用“先删后插”的幂等策略，每次发布/更新后重建当前房源的图片明细，避免脏数据。</li>
     * </ul>
     *
     * @param houseId     房源 ID
     * @param imagesJson  图片 JSON 字符串，期望格式如：["/api/uploads/a.jpg","/api/uploads/b.jpg"]
     */
    private void syncHouseImages(Long houseId, String imagesJson) {
        if (houseId == null) {
            return;
        }
        // 1) 先删除历史明细，避免重复与过期图片残留
        LambdaQueryWrapper<HouseImage> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(HouseImage::getHouseId, houseId);
        houseImageMapper.delete(deleteWrapper);

        // 2) 解析主表 JSON 字段，逐条写入明细表（含顺序）
        List<String> imageUrls = parseImageUrls(imagesJson);
        for (int i = 0; i < imageUrls.size(); i++) {
            HouseImage houseImage = new HouseImage();
            houseImage.setHouseId(houseId);
            houseImage.setImageUrl(imageUrls.get(i));
            houseImage.setSort(i);
            houseImageMapper.insert(houseImage);
        }
    }

    /**
     * 解析图片列表字符串为 URL 集合。
     *
     * <p>兼容三类输入：</p>
     * <ol>
     *   <li>标准 JSON 数组字符串：["url1","url2"]</li>
     *   <li>单个 URL 字符串：/api/uploads/a.jpg</li>
     *   <li>空值：null / 空串（返回空集合）</li>
     * </ol>
     *
     * @param images 图片字段原始值
     * @return 清洗后的图片 URL 列表（已过滤空白项）
     */
    private List<String> parseImageUrls(String images) {
        if (images == null || images.trim().isEmpty()) {
            return List.of();
        }
        String trimmed = images.trim();
        if (trimmed.startsWith("[")) {
            try {
                List<String> parsed = objectMapper.readValue(trimmed, new TypeReference<List<String>>() {});
                return parsed.stream()
                        .filter(url -> url != null && !url.trim().isEmpty())
                        .toList();
            } catch (Exception e) {
                log.warn("解析房源图片 JSON 失败，回退到单 URL 模式：{}", e.getMessage());
            }
        }
        return List.of(trimmed);
    }

    /**
     * 删除房源，同时清理关联的图片明细和用户收藏行为记录。
     * <p>操作人必须是该房源的所有者，否则抛出 403 业务异常。</p>
     *
     * @param id      要删除的房源 ID
     * @param ownerId 操作人用户 ID
     */
    @Override
    @Transactional
    @CacheEvict(value = "hotHouses", allEntries = true) // 删除房源后清除热门房源缓存
    public void deleteHouse(Long id, Long ownerId) { // 删除房源及关联数据
        House existing = houseMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "房源不存在");
        }
        // 验证操作人是否是该房源的所有者
        if (!existing.getOwnerId().equals(ownerId)) {
            throw new BusinessException(403, "没有权限删除该房源");
        }
        long relatedContractCount = contractMapper.selectCount(
                new LambdaQueryWrapper<Contract>().eq(Contract::getHouseId, id)
        );
        if (relatedContractCount > 0) {
            throw new BusinessException(400, "该房源存在关联合同，无法删除");
        }
        // 清理关联的图片明细记录
        LambdaQueryWrapper<HouseImage> imageWrapper = new LambdaQueryWrapper<>();
        imageWrapper.eq(HouseImage::getHouseId, id);
        houseImageMapper.delete(imageWrapper);
        // 清理用户对该房源的收藏行为记录
        LambdaQueryWrapper<UserBehavior> behaviorWrapper = new LambdaQueryWrapper<>();
        behaviorWrapper.eq(UserBehavior::getHouseId, id)
                .eq(UserBehavior::getBehaviorType, BEHAVIOR_COLLECT);
        userBehaviorMapper.delete(behaviorWrapper);
        // 删除房源主记录
        try {
            houseMapper.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(400, "该房源存在关联合同，无法删除");
        }
        log.info("房源 {} 已被房东 {} 删除", id, ownerId);
    }

    /**
     * 房东主动上架自己的房源。
     * <p>安全约束：</p>
     * <ul>
     *   <li>房源必须存在，否则返回 404。</li>
     *   <li>仅房源所有者可操作，否则返回 403。</li>
     * </ul>
     * <p>业务效果：将 status 设置为 ONLINE，并刷新更新时间。</p>
     *
     * @param id      房源 ID
     * @param ownerId 当前操作房东 ID
     */
    @Override
    @Transactional
    @CacheEvict(value = "hotHouses", allEntries = true)
    public void putHouseOnline(Long id, Long ownerId) { // 房东主动上架房源
        House existing = houseMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "房源不存在");
        }
        if (!existing.getOwnerId().equals(ownerId)) {
            throw new BusinessException(403, "没有权限操作该房源");
        }
        existing.setStatus(HOUSE_STATUS_ONLINE);
        existing.setUpdateTime(LocalDateTime.now());
        houseMapper.updateById(existing);
    }

    /**
     * 房东主动下架自己的房源。
     * <p>安全约束：</p>
     * <ul>
     *   <li>房源必须存在，否则返回 404。</li>
     *   <li>仅房源所有者可操作，否则返回 403。</li>
     * </ul>
     * <p>业务效果：将 status 设置为 OFFLINE，并刷新更新时间。</p>
     *
     * @param id      房源 ID
     * @param ownerId 当前操作房东 ID
     */
    @Override
    @Transactional
    @CacheEvict(value = "hotHouses", allEntries = true)
    public void putHouseOffline(Long id, Long ownerId) { // 房东主动下架房源
        House existing = houseMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "房源不存在");
        }
        if (!existing.getOwnerId().equals(ownerId)) {
            throw new BusinessException(403, "没有权限操作该房源");
        }
        existing.setStatus(HOUSE_STATUS_OFFLINE);
        existing.setUpdateTime(LocalDateTime.now());
        houseMapper.updateById(existing);
    }

    /**
     * 查询房源详情，同时尝试增加浏览量（失败不影响主流程）。
     * 图片列表优先从 house_images 明细表读取并重建 images JSON 字段，
     * 确保 house_images 表中的排序信息得到实际使用。
     *
     * @param id 房源 ID
     * @return 房源详情对象
     */
    @Override
    public House getHouseById(Long id) { // 查询房源详情并记录浏览行为
        House house = houseMapper.selectById(id);
        if (house == null) {
            throw new BusinessException(404, "房源不存在");
        }
        // 尝试增加浏览量，失败时只打印警告不影响正常查询
        try {
            houseMapper.incrementViewCount(id);
        } catch (Exception e) {
            log.warn("增加浏览量失败：{}", e.getMessage());
        }
        // 从 house_images 明细表读取图片列表（按 sort 升序），重建 images JSON 字段。
        // house_images 通过 syncHouseImages 在写入时保持与 houses.images 同步，
        // 因此两者正常情况下始终一致；当 house_images 无数据时（如历史旧数据），
        // 保留 houses.images 原值作为兜底。
        try {
            List<HouseImage> houseImages = houseImageMapper.selectByHouseId(id);
            if (!houseImages.isEmpty()) {
                List<String> urls = houseImages.stream()
                        .map(HouseImage::getImageUrl)
                        .toList();
                house.setImages(objectMapper.writeValueAsString(urls));
            }
        } catch (Exception e) {
            log.warn("从 house_images 填充房源 {} 图片失败：{}", id, e.getMessage());
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
     * 查询指定房源的图片列表（从 house_images 明细表读取，按 sort 升序）
     *
     * @param houseId 房源 ID
     * @return 该房源的图片列表
     */
    @Override
    public List<HouseImage> getHouseImages(Long houseId) { // 查询房源图片明细列表
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            throw new BusinessException(404, "房源不存在");
        }
        return houseImageMapper.selectByHouseId(houseId);
    }

    /**
     * 按条件分页搜索房源，主查询失败时降级为简单查询
     *
     * @param request 搜索请求参数
     * @return 符合条件的分页房源列表
     */
    @Override
    public PageResult<House> searchHouses(HouseSearchRequest request) { // 多条件分页搜索房源
        Page<House> page = new Page<>(request.getPage(), request.getSize());
        try {
            // 使用 XML Mapper 中的复杂条件查询
            com.baomidou.mybatisplus.core.metadata.IPage<House> result = houseMapper.selectByCondition(page, request);
            return PageResult.of(result.getTotal(), result.getRecords(), request.getPage(), request.getSize());
        } catch (Exception e) {
            // 降级处理：复杂查询失败时退回简单查询
            log.error("复杂的房屋搜索出错了，系统正在改用基础搜索方式重试: {}", e.getMessage());
            LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(House::getStatus, HOUSE_STATUS_ONLINE);
            // 降级分支也要保持与 XML 主查询一致的排序语义，避免主查询异常时“最新”按钮失效。
            // newest = 最近更新时间优先；其余保持原有默认行为（最新发布时间优先）。
            if ("newest".equals(request.getSortBy())) {
                wrapper.orderByDesc(House::getUpdateTime);
            } else if ("price_asc".equals(request.getSortBy())) {
                wrapper.orderByAsc(House::getPrice);
            } else if ("price_desc".equals(request.getSortBy())) {
                wrapper.orderByDesc(House::getPrice);
            } else if ("popular".equals(request.getSortBy())) {
                wrapper.orderByDesc(House::getViewCount);
            } else {
                wrapper.orderByDesc(House::getCreateTime);
            }
            Page<House> result = houseMapper.selectPage(page, wrapper);
            return PageResult.of(result.getTotal(), result.getRecords(), request.getPage(), request.getSize());
        }
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
    public PageResult<House> listOwnerHouses(Long ownerId, int page, int size) { // 查询房东发布房源分页
        Page<House> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(House::getOwnerId, ownerId);
        wrapper.orderByDesc(House::getCreateTime);
        Page<House> result = houseMapper.selectPage(pageObj, wrapper);
        return PageResult.of(result.getTotal(), result.getRecords(), page, size);
    }

    /**
     * 查询用户收藏的房源列表，按收藏时间倒序返回
     *
     * @param userId 用户 ID
     * @param page   当前页码
     * @param size   每页大小
     * @return 收藏房源分页结果
     */
    @Override
    public PageResult<House> listCollectedHouses(Long userId, int page, int size) { // 查询用户收藏房源分页
        Page<UserBehavior> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<UserBehavior> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBehavior::getUserId, userId)
                .eq(UserBehavior::getBehaviorType, BEHAVIOR_COLLECT)
                .orderByDesc(UserBehavior::getCreateTime);
        Page<UserBehavior> behaviorPage = userBehaviorMapper.selectPage(pageObj, wrapper);
        List<Long> houseIds = behaviorPage.getRecords().stream()
                .map(UserBehavior::getHouseId)
                .toList();
        List<House> houses = houseIds.isEmpty() ? List.of() : houseMapper.selectBatchIds(houseIds);
        Map<Long, House> houseMap = houses.stream().collect(Collectors.toMap(House::getId, h -> h, (a, b) -> a));
        List<House> ordered = houseIds.stream()
                .map(houseMap::get)
                .filter(h -> h != null && HOUSE_STATUS_ONLINE.equals(h.getStatus()))
                .toList();
        return PageResult.of(behaviorPage.getTotal(), ordered, (int) behaviorPage.getCurrent(), (int) behaviorPage.getSize());
    }

    /**
     * 用户收藏房源，若已收藏则忽略（幂等操作）
     *
     * @param userId  用户 ID
     * @param houseId 房源 ID
     */
    @Override
    @Transactional
    public void collectHouse(Long userId, Long houseId) { // 收藏房源并记录行为分
        // 检查是否已经收藏过该房源
        LambdaQueryWrapper<UserBehavior> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBehavior::getUserId, userId)
                .eq(UserBehavior::getHouseId, houseId)
                .eq(UserBehavior::getBehaviorType, BEHAVIOR_COLLECT);
        UserBehavior existing = userBehaviorMapper.selectOne(wrapper);
        if (existing == null) {
            // 未收藏过：新增一条收藏行为，并显式写入 score=3。
            // 之前未写 score 会导致推荐行为权重丢失，本次统一补齐。
            UserBehavior behavior = new UserBehavior();
            behavior.setUserId(userId);
            behavior.setHouseId(houseId);
            behavior.setBehaviorType(BEHAVIOR_COLLECT);
            behavior.setScore(BEHAVIOR_COLLECT_SCORE);
            behavior.setCreateTime(LocalDateTime.now());
            userBehaviorMapper.insert(behavior);
            return;
        }
        // 已收藏过：保持接口幂等（不新增重复记录）。
        // 但为了兼容历史数据，若旧记录 score 为空或非 3，则在本次请求中修正为 3。
        if (existing.getScore() == null || existing.getScore().compareTo(BEHAVIOR_COLLECT_SCORE) != 0) {
            existing.setScore(BEHAVIOR_COLLECT_SCORE);
            userBehaviorMapper.updateById(existing);
        }
    }

    /**
     * 取消收藏房源，若未收藏则忽略
     *
     * @param userId  用户 ID
     * @param houseId 房源 ID
     */
    @Override
    @Transactional
    public void cancelCollectHouse(Long userId, Long houseId) { // 取消收藏并清理行为记录
        LambdaQueryWrapper<UserBehavior> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBehavior::getUserId, userId)
                .eq(UserBehavior::getHouseId, houseId)
                .eq(UserBehavior::getBehaviorType, BEHAVIOR_COLLECT);
        userBehaviorMapper.delete(wrapper);
    }

    /**
     * 获取热门房源列表（按浏览量降序，最多 10 条），结果缓存到 Redis
     *
     * @return 热门房源列表
     */
    @Override
    @Cacheable(value = "hotHouses", key = "'all'") // 缓存热门房源列表，key 为 'hotHouses::all'
    public List<House> getHotHouses() { // 获取热门房源（带缓存）
        try {
            LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(House::getStatus, HOUSE_STATUS_ONLINE);
            wrapper.orderByDesc(House::getViewCount);
            Page<House> page = new Page<>(1, 10);
            return houseMapper.selectPage(page, wrapper).getRecords();
        } catch (Exception e) {
            log.error("获取热门房源失败：{}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 增加指定房源的浏览量，失败时记录日志不抛出异常
     *
     * @param houseId 房源 ID
     */
    @Override
    public void incrementViewCount(Long houseId) { // 增加房源浏览量
        try {
            houseMapper.incrementViewCount(houseId);
        } catch (Exception e) {
            log.warn("增加房源 {} 浏览量失败：{}", houseId, e.getMessage());
        }
    }
}
