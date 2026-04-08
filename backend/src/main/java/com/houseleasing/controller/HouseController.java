package com.houseleasing.controller;

import com.houseleasing.common.PageResult;
import com.houseleasing.common.Result;
import com.houseleasing.common.exception.BusinessException;
import com.houseleasing.dto.HouseSearchRequest;
import com.houseleasing.entity.House;
import com.houseleasing.entity.HouseImage;
import com.houseleasing.entity.User;
import com.houseleasing.mapper.HouseMapper;
import com.houseleasing.mapper.OrderMapper;
import com.houseleasing.mapper.UserMapper;
import com.houseleasing.service.HouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 房源管理控制器
 *
 * @author hongwenhao
 * @description 提供房源相关的 REST API，包括公开的搜索查询接口和需要认证的房源发布、更新、收藏接口
 */
@Tag(name = "House", description = "House management")
@RestController
@RequestMapping("/api/houses") // 房源接口统一前缀
@RequiredArgsConstructor // 自动注入依赖对象
public class HouseController { // 处理房源搜索、发布、上下架、收藏等功能

    private final HouseService houseService; // 房源业务服务
    private final UserMapper userMapper; // 用户查询组件
    private final HouseMapper houseMapper; // 房源数据库访问组件
    private final OrderMapper orderMapper; // 订单数据库访问组件

    /**
     * 按条件搜索房源（公开接口，无需认证）
     *
     * @param request 搜索条件和分页参数
     * @return 符合条件的分页房源列表
     */
    @Operation(summary = "Search houses (public)")
    @GetMapping("/search")
    public Result<PageResult<House>> searchHouses(HouseSearchRequest request) { // 按筛选条件搜索房源
        return Result.success(houseService.searchHouses(request)); // 返回分页搜索结果
    }

    /**
     * 获取所有在线房源列表（公开接口）
     *
     * @param page 当前页码，默认第 1 页
     * @param size 每页大小，默认 10 条
     * @return 分页房源列表
     */
    @Operation(summary = "Get all houses (public)")
    @GetMapping
    public Result<PageResult<House>> listHouses(HouseSearchRequest request) { // 查询公开房源列表
        try {
            request.normalizePagination(); // 统一处理分页参数（page/size 等）
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, "Conflicting pagination parameters provided"); // 分页参数冲突时给出可读错误
        }
        return Result.success(houseService.searchHouses(request)); // 复用搜索逻辑返回列表
    }

    /**
     * 根据房源 ID 查询房源详情（公开接口，同时增加浏览量）
     *
     * @param id 房源 ID
     * @return 房源详情信息
     */
    @Operation(summary = "Get house by ID (public)")
    @GetMapping("/{id}")
    public Result<House> getHouseById(@PathVariable Long id) { // 查询单个房源详情
        return Result.success(houseService.getHouseById(id)); // 返回该房源完整信息
    }

    /**
     * 查询指定房源的图片列表（公开接口，从 house_images 明细表读取，按排序升序）
     *
     * @param id 房源 ID
     * @return 该房源的图片列表
     */
    @Operation(summary = "Get images for a house (public)")
    @GetMapping("/{id}/images")
    public Result<List<HouseImage>> getHouseImages(@PathVariable Long id) { // 查询房源图片列表
        return Result.success(houseService.getHouseImages(id)); // 返回按顺序排列的图片
    }

    /**
     * 获取热门房源列表（按浏览量排序，使用 Redis 缓存）
     *
     * @return 热门房源列表
     */
    @Operation(summary = "Get hot houses")
    @GetMapping("/hot")
    public Result<List<House>> getHotHouses() { // 查询热门房源
        return Result.success(houseService.getHotHouses()); // 返回按热度排序的房源
    }

    /**
     * 获取首页公开统计数据（在租房源、注册用户、成交数量、覆盖城市）
     *
     * @return 首页统计数据
     */
    @Operation(summary = "Get home statistics (public)")
    @GetMapping("/home-stats")
    public Result<Map<String, Long>> getHomeStats() { // 首页统计数据接口
        Map<String, Long> stats = new HashMap<>(); // 用于返回多个统计项
        stats.put("houses", houseMapper.countOnlineHouses()); // 在租房源数量
        stats.put("users", userMapper.selectCount(null)); // 注册用户总数
        // 成交口径统一为“订单已完成”，仅 COMPLETED 才计入交易成功次数。
        stats.put("deals", orderMapper.countCompletedOrders()); // 成交数量
        stats.put("cities", houseMapper.countOnlineCities()); // 覆盖城市数
        return Result.success(stats); // 返回首页展示所需统计
    }

    /**
     * 房东发布新房源（需要认证）
     *
     * @param house       房源信息
     * @param userDetails 当前登录用户信息
     * @return 发布成功的房源对象
     */
    @Operation(summary = "Add new house")
    @PostMapping
    @SecurityRequirement(name = "Bearer Authentication")
    public Result<House> addHouse(@RequestBody House house,
                                   @AuthenticationPrincipal UserDetails userDetails) { // 房东发布房源
        User user = resolveUser(userDetails.getUsername()); // 获取当前登录房东
        return Result.success(houseService.addHouse(house, user.getId())); // 保存新房源并返回
    }

    /**
     * 房东更新房源信息（需要认证，且只能修改自己的房源）
     *
     * @param id          要更新的房源 ID
     * @param house       更新的房源信息
     * @param userDetails 当前登录用户信息
     * @return 更新后的房源对象
     */
    @Operation(summary = "Update house")
    @PutMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    public Result<House> updateHouse(@PathVariable Long id,
                                       @RequestBody House house,
                                       @AuthenticationPrincipal UserDetails userDetails) { // 修改已有房源信息
        User user = resolveUser(userDetails.getUsername()); // 获取当前用户
        return Result.success(houseService.updateHouse(id, house, user.getId())); // 按权限更新并返回最新数据
    }

    /**
     * 查询当前房东发布的所有房源（需要认证）
     *
     * @param userDetails 当前登录用户信息
     * @param page        当前页码
     * @param size        每页大小
     * @return 当前房东的分页房源列表
     */
    @Operation(summary = "List owner's houses")
    @GetMapping("/my")
    @SecurityRequirement(name = "Bearer Authentication")
    public Result<PageResult<House>> listMyHouses(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) { // 查询我发布的房源
        User user = resolveUser(userDetails.getUsername()); // 解析当前房东
        return Result.success(houseService.listOwnerHouses(user.getId(), page, size)); // 返回房东房源分页
    }

    /**
     * 查询当前用户收藏的房源列表（需要认证）
     *
     * @param userDetails 当前登录用户信息
     * @param page        当前页码
     * @param size        每页大小
     * @return 收藏房源的分页列表
     */
    @Operation(summary = "List user's collected houses")
    @GetMapping("/my/collections")
    @SecurityRequirement(name = "Bearer Authentication")
    public Result<PageResult<House>> listCollectedHouses(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) { // 查询我收藏的房源
        User user = resolveUser(userDetails.getUsername()); // 获取当前用户
        ensureTenantUser(user); // 仅租客允许收藏功能
        return Result.success(houseService.listCollectedHouses(user.getId(), page, size)); // 返回收藏分页列表
    }

    /**
     * 收藏指定房源（需要认证）
     *
     * @param id          要收藏的房源 ID
     * @param userDetails 当前登录用户信息
     * @return 操作成功的响应
     */
    @Operation(summary = "Collect/favorite a house")
    @PostMapping("/{id}/collect")
    @SecurityRequirement(name = "Bearer Authentication")
    public Result<Void> collectHouse(@PathVariable Long id,
                                      @AuthenticationPrincipal UserDetails userDetails) { // 收藏指定房源
        User user = resolveUser(userDetails.getUsername()); // 获取当前用户
        ensureTenantUser(user); // 非租客不允许收藏
        houseService.collectHouse(user.getId(), id); // 执行收藏
        return Result.success(); // 返回成功
    }

    /**
     * 删除指定房源（需要认证，且只能删除自己的房源）
     *
     * @param id          要删除的房源 ID
     * @param userDetails 当前登录用户信息
     * @return 操作成功的响应
     */
    @Operation(summary = "Delete house")
    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    public Result<Void> deleteHouse(@PathVariable Long id,
                                     @AuthenticationPrincipal UserDetails userDetails) { // 删除我自己的房源
        User user = resolveUser(userDetails.getUsername()); // 获取当前房东
        houseService.deleteHouse(id, user.getId()); // 按权限删除房源
        return Result.success(); // 返回成功
    }

    /**
     * 房东主动上架自己的房源（需要认证，且只能操作自己的房源）。
     *
     * @param id          房源 ID
     * @param userDetails 当前登录用户信息
     * @return 操作成功响应
     */
    @Operation(summary = "Put own house online")
    @PutMapping("/{id}/online")
    @SecurityRequirement(name = "Bearer Authentication")
    public Result<Void> putMyHouseOnline(@PathVariable Long id,
                                         @AuthenticationPrincipal UserDetails userDetails) { // 房东主动上架房源
        User user = resolveUser(userDetails.getUsername()); // 解析当前用户
        houseService.putHouseOnline(id, user.getId()); // 更新房源为 ONLINE
        return Result.success(); // 返回成功
    }

    /**
     * 房东主动下架自己的房源（需要认证，且只能操作自己的房源）。
     *
     * @param id          房源 ID
     * @param userDetails 当前登录用户信息
     * @return 操作成功响应
     */
    @Operation(summary = "Put own house offline")
    @PutMapping("/{id}/offline")
    @SecurityRequirement(name = "Bearer Authentication")
    public Result<Void> putMyHouseOffline(@PathVariable Long id,
                                           @AuthenticationPrincipal UserDetails userDetails) { // 房东主动下架房源
        User user = resolveUser(userDetails.getUsername()); // 解析当前用户
        houseService.putHouseOffline(id, user.getId()); // 更新房源为 OFFLINE
        return Result.success(); // 返回成功
    }

    /**
     * 取消收藏指定房源（需要认证）
     *
     * @param id          要取消收藏的房源 ID
     * @param userDetails 当前登录用户信息
     * @return 操作成功的响应
     */
    @Operation(summary = "Cancel favorite for a house")
    @DeleteMapping("/{id}/collect")
    @SecurityRequirement(name = "Bearer Authentication")
    public Result<Void> cancelCollectHouse(@PathVariable Long id,
                                            @AuthenticationPrincipal UserDetails userDetails) { // 取消收藏房源
        User user = resolveUser(userDetails.getUsername()); // 获取当前用户
        ensureTenantUser(user); // 仅租客可操作
        houseService.cancelCollectHouse(user.getId(), id); // 执行取消收藏
        return Result.success(); // 返回成功
    }

    /**
     * 根据用户名解析用户信息
     *
     * @param username 用户名
     * @return 对应的用户实体
     */
    private User resolveUser(String username) { // 工具方法：根据用户名获取用户
        User user = userMapper.selectByUsername(username); // 查询数据库
        if (user == null) { // 用户不存在时
            throw new BusinessException(404, "用户不存在"); // 抛出业务错误
        }
        return user; // 返回用户实体
    }

    /**
     * 仅允许租客使用收藏相关接口，房东/管理员直接拒绝
     *
     * @param user 当前认证用户
     */
    private void ensureTenantUser(User user) { // 校验当前用户是否是租客
        if (!"TENANT".equalsIgnoreCase(user.getRole())) { // 不是租客则拒绝
            // 返回 403 业务码，提示只有租客可以收藏房源
            throw new BusinessException(403, "仅租客可以收藏房源"); // 给出明确权限提示
        }
    }
}
