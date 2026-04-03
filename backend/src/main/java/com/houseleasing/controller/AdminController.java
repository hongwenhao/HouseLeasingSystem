package com.houseleasing.controller;

import com.houseleasing.common.PageResult;
import com.houseleasing.common.Result;
import com.houseleasing.entity.Contract;
import com.houseleasing.entity.House;
import com.houseleasing.entity.Order;
import com.houseleasing.entity.User;
import com.houseleasing.mapper.ContractMapper;
import com.houseleasing.mapper.HouseMapper;
import com.houseleasing.mapper.OrderMapper;
import com.houseleasing.mapper.UserMapper;
import com.houseleasing.service.HouseService;
import com.houseleasing.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 后台管理控制器
 *
 * @author HouseLeasingSystem开发团队
 * @description 提供系统管理员专用的后台管理 REST API，包括用户管理、房源审核、
 * 订单查看、合同查看和系统统计，所有接口仅限 ADMIN 角色访问
 */
@Tag(name = "Admin", description = "Admin management endpoints")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')") // 仅允许管理员角色访问此控制器的所有接口
public class AdminController {

    private final UserService userService;
    private final HouseService houseService;
    private final UserMapper userMapper;
    private final HouseMapper houseMapper;
    private final OrderMapper orderMapper;
    private final ContractMapper contractMapper;

    /**
     * 查询系统所有用户列表（支持关键词搜索）
     *
     * @param page    当前页码
     * @param size    每页大小
     * @param keyword 搜索关键词（可选）
     * @return 分页用户列表
     */
    @Operation(summary = "List all users")
    @GetMapping("/users")
    public Result<PageResult<User>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.success(userService.listUsers(page, size, keyword));
    }

    /**
     * 封禁指定用户账号
     *
     * @param id 要封禁的用户 ID
     * @return 操作成功的响应
     */
    @Operation(summary = "Ban user")
    @PutMapping("/users/{id}/ban")
    public Result<Void> banUser(@PathVariable Long id) {
        userService.banUser(id);
        return Result.success();
    }

    /**
     * 解封指定用户账号
     *
     * @param id 要解封的用户 ID
     * @return 操作成功的响应
     */
    @Operation(summary = "Unban user")
    @PutMapping("/users/{id}/unban")
    public Result<Void> unbanUser(@PathVariable Long id) {
        userService.unbanUser(id);
        return Result.success();
    }

    /**
     * 查询系统所有订单列表（分页）
     *
     * @param page 当前页码
     * @param size 每页大小
     * @return 所有订单的分页列表
     */
    @Operation(summary = "List all orders")
    @GetMapping("/orders")
    public Result<PageResult<Order>> listAllOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Order> pageObj =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.orderByDesc(Order::getCreateTime);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Order> result =
                orderMapper.selectPage(pageObj, wrapper);
        return Result.success(PageResult.of(result.getTotal(), result.getRecords(), page, size));
    }

    /**
     * 查询系统所有合同列表（分页）
     *
     * @param page 当前页码
     * @param size 每页大小
     * @return 所有合同的分页列表
     */
    @Operation(summary = "List all contracts")
    @GetMapping("/contracts")
    public Result<PageResult<Contract>> listAllContracts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Contract> pageObj =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Contract> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.orderByDesc(Contract::getCreateTime);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Contract> result =
                contractMapper.selectPage(pageObj, wrapper);
        return Result.success(PageResult.of(result.getTotal(), result.getRecords(), page, size));
    }

    /**
     * 获取系统统计数据（用户总数、房源总数、订单总数、合同总数）
     *
     * @return 包含各项统计数据的 Map
     */
    @Operation(summary = "Get system statistics")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userMapper.selectCount(null));     // 用户总数
        stats.put("houseCount", houseMapper.selectCount(null));   // 房源总数
        stats.put("orderCount", orderMapper.selectCount(null));   // 订单总数
        // 合同统计：总数及待签署/审核数量
        stats.put("contractCount", contractMapper.selectCount(null)); // 合同总数
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Contract> pendingWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        pendingWrapper.eq(Contract::getStatus, "PENDING_SIGN");
        stats.put("pendingContracts", contractMapper.selectCount(pendingWrapper)); // 待审核合同数
        return Result.success(stats);
    }
}
