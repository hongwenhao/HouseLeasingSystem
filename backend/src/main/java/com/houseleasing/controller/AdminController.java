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

@Tag(name = "Admin", description = "Admin management endpoints")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final HouseService houseService;
    private final UserMapper userMapper;
    private final HouseMapper houseMapper;
    private final OrderMapper orderMapper;
    private final ContractMapper contractMapper;

    @Operation(summary = "List all users")
    @GetMapping("/users")
    public Result<PageResult<User>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.success(userService.listUsers(page, size, keyword));
    }

    @Operation(summary = "Ban user")
    @PutMapping("/users/{id}/ban")
    public Result<Void> banUser(@PathVariable Long id) {
        userService.banUser(id);
        return Result.success();
    }

    @Operation(summary = "Unban user")
    @PutMapping("/users/{id}/unban")
    public Result<Void> unbanUser(@PathVariable Long id) {
        userService.unbanUser(id);
        return Result.success();
    }

    @Operation(summary = "List pending approval houses")
    @GetMapping("/houses/pending")
    public Result<PageResult<House>> listPendingHouses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        com.houseleasing.dto.HouseSearchRequest req = new com.houseleasing.dto.HouseSearchRequest();
        req.setPage(page);
        req.setSize(size);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<House> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(House::getStatus, "PENDING").orderByDesc(House::getCreateTime);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<House> pageObj =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<House> result =
                houseMapper.selectPage(pageObj, wrapper);
        return Result.success(PageResult.of(result.getTotal(), result.getRecords(), page, size));
    }

    @Operation(summary = "Approve house")
    @PutMapping("/houses/{id}/approve")
    public Result<Void> approveHouse(@PathVariable Long id,
                                      @RequestBody(required = false) Map<String, String> request) {
        String reason = request != null ? request.get("reason") : null;
        houseService.approveHouse(id, true, reason);
        return Result.success();
    }

    @Operation(summary = "Reject house")
    @PutMapping("/houses/{id}/reject")
    public Result<Void> rejectHouse(@PathVariable Long id,
                                     @RequestBody(required = false) Map<String, String> request) {
        String reason = request != null ? request.get("reason") : null;
        houseService.approveHouse(id, false, reason);
        return Result.success();
    }

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

    @Operation(summary = "Get system statistics")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userMapper.selectCount(null));
        stats.put("houseCount", houseMapper.selectCount(null));
        stats.put("orderCount", orderMapper.selectCount(null));
        stats.put("contractCount", contractMapper.selectCount(null));
        return Result.success(stats);
    }
}
