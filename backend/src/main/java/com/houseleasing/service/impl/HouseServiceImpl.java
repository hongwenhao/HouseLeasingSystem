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
    private static final BigDecimal BEHAVIOR_COLLECT_SCORE = new BigDecimal("3.0"); // 创建对象承载本步骤数据
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
        if (owner == null) { // 在该判断成立时执行对应逻辑
            throw new BusinessException(404, "用户不存在"); // 立即返回错误避免继续执行
        }
        if (owner.getCreditScore() != null && owner.getCreditScore() < CREDIT_SCORE_PUBLISHING_THRESHOLD) { // 在该判断成立时执行对应逻辑
            throw new BusinessException(403, "当前信用分过低，暂不可发布房源"); // 以异常形式提示调用方当前问题
        }
        house.setOwnerId(ownerId); // 绑定房源归属房东
        house.setStatus(HOUSE_STATUS_ONLINE); // 新房源默认状态为已上线
        house.setViewCount(0); // 新房源浏览量从 0 开始累计
        house.setCreateTime(LocalDateTime.now()); // 写入创建时间
        house.setUpdateTime(LocalDateTime.now()); // 初始化更新时间
        houseMapper.insert(house); // 插入房源主表记录
        // 同步写入 house_images 明细表，确保“房源主表 JSON 字段”和“图片明细表”两处数据一致
        syncHouseImages(house.getId(), house.getImages()); // 借助已有方法完成该业务动作
        return house; // 把结果交还给上层调用方
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
        House existing = houseMapper.selectById(id); // 读取当前业务所需数据
        if (existing == null) { // 按该条件分支处理不同业务场景
            throw new BusinessException(404, "房源不存在"); // 抛出业务异常并中断当前流程
        }
        // 验证操作人是否是该房源的所有者
        if (!existing.getOwnerId().equals(ownerId)) { // 依据当前状态决定后续处理路径
            throw new BusinessException(403, "没有权限修改该房源"); // 抛出业务异常并中断当前流程
        }
        house.setId(id); // 补齐对象属性供后续流程使用
        house.setOwnerId(ownerId); // 设置业务字段以形成完整数据
        house.setUpdateTime(LocalDateTime.now()); // 持久化本次状态更新
        houseMapper.updateById(house); // 调用组件能力完成当前步骤
        House updatedHouse = houseMapper.selectById(id); // 先查出目标记录再做业务判断
        // 更新后按数据库最终值重建图片明细，兼容“本次请求未携带 images 字段”的场景
        syncHouseImages(id, updatedHouse != null ? updatedHouse.getImages() : null); // 借助已有方法完成该业务动作
        return updatedHouse; // 输出本方法最终结果
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
    private void syncHouseImages(Long houseId, String imagesJson) { // 借助已有方法完成该业务动作
        if (houseId == null) { // 在该判断成立时执行对应逻辑
            return; // 按既定流程继续处理后续逻辑
        }
        // 1) 先删除历史明细，避免重复与过期图片残留
        LambdaQueryWrapper<HouseImage> deleteWrapper = new LambdaQueryWrapper<>(); // 读取当前业务所需数据
        deleteWrapper.eq(HouseImage::getHouseId, houseId); // 调用组件能力完成当前步骤
        houseImageMapper.delete(deleteWrapper); // 执行删除动作清理无效数据

        // 2) 解析主表 JSON 字段，逐条写入明细表（含顺序）
        List<String> imageUrls = parseImageUrls(imagesJson); // 调用组件能力完成当前步骤
        for (int i = 0; i < imageUrls.size(); i++) { // 遍历集合逐项处理业务数据
            HouseImage houseImage = new HouseImage(); // 创建对象承载本步骤数据
            houseImage.setHouseId(houseId); // 设置业务字段以形成完整数据
            houseImage.setImageUrl(imageUrls.get(i)); // 给对象写入当前步骤需要的字段值
            houseImage.setSort(i); // 设置业务字段以形成完整数据
            houseImageMapper.insert(houseImage); // 把新建数据写入数据库
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
    private List<String> parseImageUrls(String images) { // 调用组件能力完成当前步骤
        if (images == null || images.trim().isEmpty()) { // 在该判断成立时执行对应逻辑
            return List.of(); // 把结果交还给上层调用方
        }
        String trimmed = images.trim(); // 执行对应服务/DAO方法推进流程
        if (trimmed.startsWith("[")) { // 在该判断成立时执行对应逻辑
            try { // 在当前步骤完成必要业务动作
                List<String> parsed = objectMapper.readValue(trimmed, new TypeReference<List<String>>() {}); // 实例化新对象用于后续操作
                return parsed.stream() // 输出本方法最终结果
                        .filter(url -> url != null && !url.trim().isEmpty())
                        .toList(); // 执行对应服务/DAO方法推进流程
            } catch (Exception e) { // 在当前步骤完成必要业务动作
                log.warn("解析房源图片 JSON 失败，回退到单 URL 模式：{}", e.getMessage()); // 调用组件能力完成当前步骤
            }
        }
        return List.of(trimmed); // 输出本方法最终结果
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
        House existing = houseMapper.selectById(id); // 读取当前业务所需数据
        if (existing == null) { // 按该条件分支处理不同业务场景
            throw new BusinessException(404, "房源不存在"); // 抛出业务异常并中断当前流程
        }
        // 验证操作人是否是该房源的所有者
        if (!existing.getOwnerId().equals(ownerId)) { // 依据当前状态决定后续处理路径
            throw new BusinessException(403, "没有权限删除该房源"); // 立即返回错误避免继续执行
        }
        long relatedContractCount = contractMapper.selectCount(
                new LambdaQueryWrapper<Contract>().eq(Contract::getHouseId, id)
        ); // 在当前步骤完成必要业务动作
        if (relatedContractCount > 0) { // 依据当前状态决定后续处理路径
            throw new BusinessException(400, "该房源存在关联合同，无法删除"); // 以异常形式提示调用方当前问题
        }
        // 清理关联的图片明细记录
        LambdaQueryWrapper<HouseImage> imageWrapper = new LambdaQueryWrapper<>(); // 读取当前业务所需数据
        imageWrapper.eq(HouseImage::getHouseId, id); // 借助已有方法完成该业务动作
        houseImageMapper.delete(imageWrapper); // 从数据库移除对应记录
        // 清理用户对该房源的收藏行为记录
        LambdaQueryWrapper<UserBehavior> behaviorWrapper = new LambdaQueryWrapper<>(); // 先查出目标记录再做业务判断
        behaviorWrapper.eq(UserBehavior::getHouseId, id)
                .eq(UserBehavior::getBehaviorType, BEHAVIOR_COLLECT); // 执行对应服务/DAO方法推进流程
        userBehaviorMapper.delete(behaviorWrapper); // 执行删除动作清理无效数据
        // 删除房源主记录
        try { // 在当前步骤完成必要业务动作
            houseMapper.deleteById(id); // 借助已有方法完成该业务动作
        } catch (DataIntegrityViolationException e) { // 这里执行当前语句的核心处理
            throw new BusinessException(400, "该房源存在关联合同，无法删除"); // 以异常形式提示调用方当前问题
        }
        log.info("房源 {} 已被房东 {} 删除", id, ownerId); // 借助已有方法完成该业务动作
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
        House existing = houseMapper.selectById(id); // 先读取房源，确认目标记录存在
        if (existing == null) { // 按该条件分支处理不同业务场景
            throw new BusinessException(404, "房源不存在"); // 抛出业务异常并中断当前流程
        }
        if (!existing.getOwnerId().equals(ownerId)) { // 依据当前状态决定后续处理路径
            throw new BusinessException(403, "没有权限操作该房源"); // 以异常形式提示调用方当前问题
        }
        existing.setStatus(HOUSE_STATUS_ONLINE); // 更新状态为“上架中”，使租客搜索结果可见
        existing.setUpdateTime(LocalDateTime.now()); // 刷新更新时间，便于“最新更新”排序准确
        houseMapper.updateById(existing); // 持久化状态变更到数据库
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
        House existing = houseMapper.selectById(id); // 加载房源记录，后续执行权限与状态更新
        if (existing == null) { // 按该条件分支处理不同业务场景
            throw new BusinessException(404, "房源不存在"); // 抛出业务异常并中断当前流程
        }
        if (!existing.getOwnerId().equals(ownerId)) { // 依据当前状态决定后续处理路径
            throw new BusinessException(403, "没有权限操作该房源"); // 以异常形式提示调用方当前问题
        }
        existing.setStatus(HOUSE_STATUS_OFFLINE); // 更新状态为“已下架”，从公开列表中隐藏
        existing.setUpdateTime(LocalDateTime.now()); // 记录下架时间对应的更新时间
        houseMapper.updateById(existing); // 保存下架后的房源状态
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
        House house = houseMapper.selectById(id); // 查询房源主记录，作为详情页基础数据
        if (house == null) { // 在该判断成立时执行对应逻辑
            throw new BusinessException(404, "房源不存在"); // 抛出业务异常并中断当前流程
        }
        // 尝试增加浏览量，失败时只打印警告不影响正常查询
        try { // 在当前步骤完成必要业务动作
            houseMapper.incrementViewCount(id); // 浏览详情即累计热度，支持热门排序与推荐
        } catch (Exception e) { // 在当前步骤完成必要业务动作
            log.warn("增加浏览量失败：{}", e.getMessage()); // 浏览量更新失败不应影响用户查看详情
        }
        // 从 house_images 明细表读取图片列表（按 sort 升序），重建 images JSON 字段。
        // house_images 通过 syncHouseImages 在写入时保持与 houses.images 同步，
        // 因此两者正常情况下始终一致；当 house_images 无数据时（如历史旧数据），
        // 保留 houses.images 原值作为兜底。
        try { // 在当前步骤完成必要业务动作
            List<HouseImage> houseImages = houseImageMapper.selectByHouseId(id); // 从图片明细表读取该房源所有图片
            if (!houseImages.isEmpty()) { // 在该判断成立时执行对应逻辑
                List<String> urls = houseImages.stream()
                        .map(HouseImage::getImageUrl)
                        .toList(); // 提取图片 URL 列表，准备回写到 images JSON 字段
                house.setImages(objectMapper.writeValueAsString(urls)); // 按前端既有字段格式输出，避免兼容性问题
            }
        } catch (Exception e) { // 在当前步骤完成必要业务动作
            log.warn("从 house_images 填充房源 {} 图片失败：{}", id, e.getMessage()); // 图片重建失败仅告警，保留原 images 兜底
        }
        // 关联填充房东信息（隐去密码等敏感字段）
        if (house.getOwnerId() != null) { // 按该条件分支处理不同业务场景
            User owner = userMapper.selectById(house.getOwnerId()); // 查询房东基础信息用于详情页展示
            if (owner != null) { // 按该条件分支处理不同业务场景
                User sanitized = new User(); // 构建脱敏视图对象，避免直接返回完整用户实体
                sanitized.setId(owner.getId()); // 保留房东ID供前端跳转个人页使用
                sanitized.setUsername(owner.getUsername()); // 保留昵称用于详情页展示
                sanitized.setPhone(owner.getPhone()); // 保留联系电话便于租客沟通
                sanitized.setAvatar(owner.getAvatar()); // 保留头像提升页面可读性
                sanitized.setRealName(owner.getRealName()); // 展示实名认证姓名增强信任感
                sanitized.setCreditScore(owner.getCreditScore()); // 展示信用分帮助租客评估交易风险
                sanitized.setIsRealNameAuth(owner.getIsRealNameAuth()); // 标识是否实名，辅助用户判断账号可靠性
                house.setLandlord(sanitized); // 把脱敏后的房东信息挂到房源详情返回体
            }
        }
        return house; // 返回已补齐图片与房东信息的房源详情对象
    }

    /**
     * 查询指定房源的图片列表（从 house_images 明细表读取，按 sort 升序）
     *
     * @param houseId 房源 ID
     * @return 该房源的图片列表
     */
    @Override
    public List<HouseImage> getHouseImages(Long houseId) { // 查询房源图片明细列表
        House house = houseMapper.selectById(houseId); // 先确认房源存在，避免查询无效图片数据
        if (house == null) { // 在该判断成立时执行对应逻辑
            throw new BusinessException(404, "房源不存在"); // 抛出业务异常并中断当前流程
        }
        return houseImageMapper.selectByHouseId(houseId); // 返回按 sort 排好序的图片明细列表
    }

    /**
     * 按条件分页搜索房源，主查询失败时降级为简单查询
     *
     * @param request 搜索请求参数
     * @return 符合条件的分页房源列表
     */
    @Override
    public PageResult<House> searchHouses(HouseSearchRequest request) { // 多条件分页搜索房源
        Page<House> page = new Page<>(request.getPage(), request.getSize()); // 创建分页参数，统一控制查询窗口
        try { // 在当前步骤完成必要业务动作
            // 使用 XML Mapper 中的复杂条件查询
            com.baomidou.mybatisplus.core.metadata.IPage<House> result = houseMapper.selectByCondition(page, request); // 执行主查询，支持多条件组合过滤
            return PageResult.of(result.getTotal(), result.getRecords(), request.getPage(), request.getSize()); // 按统一分页对象格式返回结果
        } catch (Exception e) { // 在当前步骤完成必要业务动作
            // 降级处理：复杂查询失败时退回简单查询
            log.error("复杂的房屋搜索出错了，系统正在改用基础搜索方式重试: {}", e.getMessage()); // 记录主查询失败原因，便于后续排查SQL或参数问题
            LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>(); // 构建降级查询条件，保障接口可用性
            wrapper.eq(House::getStatus, HOUSE_STATUS_ONLINE); // 降级查询仅返回上架房源
            // 降级分支也要保持与 XML 主查询一致的排序语义，避免主查询异常时“最新”按钮失效。
            // newest = 最近更新时间优先；其余保持原有默认行为（最新发布时间优先）。
            if ("newest".equals(request.getSortBy())) { // 依据当前状态决定后续处理路径
                wrapper.orderByDesc(House::getUpdateTime); // 执行对应服务/DAO方法推进流程
            } else if ("price_asc".equals(request.getSortBy())) { // 执行对应服务/DAO方法推进流程
                wrapper.orderByAsc(House::getPrice); // 借助已有方法完成该业务动作
            } else if ("price_desc".equals(request.getSortBy())) { // 执行对应服务/DAO方法推进流程
                wrapper.orderByDesc(House::getPrice); // 执行对应服务/DAO方法推进流程
            } else if ("popular".equals(request.getSortBy())) { // 执行对应服务/DAO方法推进流程
                wrapper.orderByDesc(House::getViewCount); // 借助已有方法完成该业务动作
            } else { // 这里执行当前语句的核心处理
                wrapper.orderByDesc(House::getCreateTime); // 执行对应服务/DAO方法推进流程
            }
            Page<House> result = houseMapper.selectPage(page, wrapper); // 从数据库加载后续处理对象
            return PageResult.of(result.getTotal(), result.getRecords(), request.getPage(), request.getSize()); // 输出本方法最终结果
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
        Page<House> pageObj = new Page<>(page, size); // 创建对象承载本步骤数据
        LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>(); // 从数据库加载后续处理对象
        wrapper.eq(House::getOwnerId, ownerId); // 执行对应服务/DAO方法推进流程
        wrapper.orderByDesc(House::getCreateTime); // 执行对应服务/DAO方法推进流程
        Page<House> result = houseMapper.selectPage(pageObj, wrapper); // 从数据库加载后续处理对象
        return PageResult.of(result.getTotal(), result.getRecords(), page, size); // 输出本方法最终结果
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
        Page<UserBehavior> pageObj = new Page<>(page, size); // 实例化新对象用于后续操作
        LambdaQueryWrapper<UserBehavior> wrapper = new LambdaQueryWrapper<>(); // 先查出目标记录再做业务判断
        wrapper.eq(UserBehavior::getUserId, userId)
                .eq(UserBehavior::getBehaviorType, BEHAVIOR_COLLECT)
                .orderByDesc(UserBehavior::getCreateTime); // 调用组件能力完成当前步骤
        Page<UserBehavior> behaviorPage = userBehaviorMapper.selectPage(pageObj, wrapper); // 读取当前业务所需数据
        List<Long> houseIds = behaviorPage.getRecords().stream()
                .map(UserBehavior::getHouseId)
                .toList(); // 执行对应服务/DAO方法推进流程
        List<House> houses = houseIds.isEmpty() ? List.of() : houseMapper.selectBatchIds(houseIds); // 从数据库加载后续处理对象
        Map<Long, House> houseMap = houses.stream().collect(Collectors.toMap(House::getId, h -> h, (a, b) -> a)); // 调用组件能力完成当前步骤
        List<House> ordered = houseIds.stream()
                .map(houseMap::get)
                .filter(h -> h != null && HOUSE_STATUS_ONLINE.equals(h.getStatus()))
                .toList(); // 执行对应服务/DAO方法推进流程
        return PageResult.of(behaviorPage.getTotal(), ordered, (int) behaviorPage.getCurrent(), (int) behaviorPage.getSize()); // 返回当前阶段的处理结果
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
        LambdaQueryWrapper<UserBehavior> wrapper = new LambdaQueryWrapper<>(); // 先查出目标记录再做业务判断
        wrapper.eq(UserBehavior::getUserId, userId)
                .eq(UserBehavior::getHouseId, houseId)
                .eq(UserBehavior::getBehaviorType, BEHAVIOR_COLLECT); // 执行对应服务/DAO方法推进流程
        UserBehavior existing = userBehaviorMapper.selectOne(wrapper); // 读取当前业务所需数据
        if (existing == null) { // 按该条件分支处理不同业务场景
            // 未收藏过：新增一条收藏行为，并显式写入 score=3。
            // 之前未写 score 会导致推荐行为权重丢失，本次统一补齐。
            UserBehavior behavior = new UserBehavior(); // 初始化对象以便填充业务字段
            behavior.setUserId(userId); // 补齐对象属性供后续流程使用
            behavior.setHouseId(houseId); // 给对象写入当前步骤需要的字段值
            behavior.setBehaviorType(BEHAVIOR_COLLECT); // 设置业务字段以形成完整数据
            behavior.setScore(BEHAVIOR_COLLECT_SCORE); // 补齐对象属性供后续流程使用
            behavior.setCreateTime(LocalDateTime.now()); // 补齐对象属性供后续流程使用
            userBehaviorMapper.insert(behavior); // 落库保存本次新增记录
            return; // 按既定流程继续处理后续逻辑
        }
        // 已收藏过：保持接口幂等（不新增重复记录）。
        // 但为了兼容历史数据，若旧记录 score 为空或非 3，则在本次请求中修正为 3。
        if (existing.getScore() == null || existing.getScore().compareTo(BEHAVIOR_COLLECT_SCORE) != 0) { // 在该判断成立时执行对应逻辑
            existing.setScore(BEHAVIOR_COLLECT_SCORE); // 设置业务字段以形成完整数据
            userBehaviorMapper.updateById(existing); // 调用组件能力完成当前步骤
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
        LambdaQueryWrapper<UserBehavior> wrapper = new LambdaQueryWrapper<>(); // 先查出目标记录再做业务判断
        wrapper.eq(UserBehavior::getUserId, userId)
                .eq(UserBehavior::getHouseId, houseId)
                .eq(UserBehavior::getBehaviorType, BEHAVIOR_COLLECT); // 执行对应服务/DAO方法推进流程
        userBehaviorMapper.delete(wrapper); // 从数据库移除对应记录
    }

    /**
     * 获取热门房源列表（按浏览量降序，最多 10 条），结果缓存到 Redis
     *
     * @return 热门房源列表
     */
    @Override
    @Cacheable(value = "hotHouses", key = "'all'") // 缓存热门房源列表，key 为 'hotHouses::all'
    public List<House> getHotHouses() { // 获取热门房源（带缓存）
        try { // 在当前步骤完成必要业务动作
            LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>(); // 从数据库加载后续处理对象
            wrapper.eq(House::getStatus, HOUSE_STATUS_ONLINE); // 执行对应服务/DAO方法推进流程
            wrapper.orderByDesc(House::getViewCount); // 借助已有方法完成该业务动作
            Page<House> page = new Page<>(1, 10); // 创建对象承载本步骤数据
            return houseMapper.selectPage(page, wrapper).getRecords(); // 输出本方法最终结果
        } catch (Exception e) { // 在当前步骤完成必要业务动作
            log.error("获取热门房源失败：{}", e.getMessage()); // 执行对应服务/DAO方法推进流程
            return List.of(); // 把结果交还给上层调用方
        }
    }

    /**
     * 增加指定房源的浏览量，失败时记录日志不抛出异常
     *
     * @param houseId 房源 ID
     */
    @Override
    public void incrementViewCount(Long houseId) { // 增加房源浏览量
        try { // 在当前步骤完成必要业务动作
            houseMapper.incrementViewCount(houseId); // 从数据库加载后续处理对象
        } catch (Exception e) { // 在当前步骤完成必要业务动作
            log.warn("增加房源 {} 浏览量失败：{}", houseId, e.getMessage()); // 调用组件能力完成当前步骤
        }
    }
}
