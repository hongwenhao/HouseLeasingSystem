package com.houseleasing.controller;

import com.houseleasing.common.PageResult;
import com.houseleasing.common.Result;
import com.houseleasing.common.exception.BusinessException;
import com.houseleasing.dto.HouseSearchRequest;
import com.houseleasing.entity.House;
import com.houseleasing.entity.User;
import com.houseleasing.mapper.UserMapper;
import com.houseleasing.service.HouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 房源管理控制器
 *
 * @author HouseLeasingSystem开发团队
 * @description 提供房源相关的 REST API，包括公开的搜索查询接口和需要认证的房源发布、更新、收藏接口
 */
@Tag(name = "House", description = "House management")
@RestController
@RequestMapping("/api/houses")
@RequiredArgsConstructor
public class HouseController {

    private final HouseService houseService;
    private final UserMapper userMapper;

    /**
     * 按条件搜索房源（公开接口，无需认证）
     *
     * @param request 搜索条件和分页参数
     * @return 符合条件的分页房源列表
     */
    @Operation(summary = "Search houses (public)")
    @GetMapping("/search")
    public Result<PageResult<House>> searchHouses(HouseSearchRequest request) {
        return Result.success(houseService.searchHouses(request));
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
    public Result<PageResult<House>> listHouses(HouseSearchRequest request) {
        try {
            request.normalizePagination();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, "Conflicting pagination parameters provided");
        }
        return Result.success(houseService.searchHouses(request));
    }

    /**
     * 根据房源 ID 查询房源详情（公开接口，同时增加浏览量）
     *
     * @param id 房源 ID
     * @return 房源详情信息
     */
    @Operation(summary = "Get house by ID (public)")
    @GetMapping("/{id}")
    public Result<House> getHouseById(@PathVariable Long id) {
        return Result.success(houseService.getHouseById(id));
    }

    /**
     * 获取热门房源列表（按浏览量排序，使用 Redis 缓存）
     *
     * @return 热门房源列表
     */
    @Operation(summary = "Get hot houses")
    @GetMapping("/hot")
    public Result<List<House>> getHotHouses() {
        return Result.success(houseService.getHotHouses());
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
                                   @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        return Result.success(houseService.addHouse(house, user.getId()));
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
                                      @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        return Result.success(houseService.updateHouse(id, house, user.getId()));
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
            @RequestParam(defaultValue = "10") int size) {
        User user = resolveUser(userDetails.getUsername());
        return Result.success(houseService.listOwnerHouses(user.getId(), page, size));
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
            @RequestParam(defaultValue = "10") int size) {
        User user = resolveUser(userDetails.getUsername());
        ensureTenantUser(user);
        return Result.success(houseService.listCollectedHouses(user.getId(), page, size));
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
                                      @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        ensureTenantUser(user);
        houseService.collectHouse(user.getId(), id);
        return Result.success();
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
                                           @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        ensureTenantUser(user);
        houseService.cancelCollectHouse(user.getId(), id);
        return Result.success();
    }

    /**
     * 根据用户名解析用户信息
     *
     * @param username 用户名
     * @return 对应的用户实体
     */
    private User resolveUser(String username) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    /**
     * 仅允许租客使用收藏相关接口，房东/管理员直接拒绝
     *
     * @param user 当前认证用户
     */
    private void ensureTenantUser(User user) {
        if (!"TENANT".equalsIgnoreCase(user.getRole())) {
            // 返回 403 业务码，提示只有租客可以收藏房源
            throw new BusinessException(403, "仅租客可以收藏房源");
        }
    }
}
