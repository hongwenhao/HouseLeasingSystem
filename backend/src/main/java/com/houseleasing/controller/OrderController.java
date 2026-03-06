package com.houseleasing.controller;

import com.houseleasing.common.PageResult;
import com.houseleasing.common.Result;
import com.houseleasing.common.exception.BusinessException;
import com.houseleasing.dto.OrderCreateRequest;
import com.houseleasing.entity.Order;
import com.houseleasing.entity.User;
import com.houseleasing.mapper.UserMapper;
import com.houseleasing.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Order", description = "Order management")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class OrderController {

    private final OrderService orderService;
    private final UserMapper userMapper;

    @Operation(summary = "Create intent order")
    @PostMapping("/intent")
    public Result<Order> createIntent(@RequestBody Map<String, Object> request,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        Long houseId = Long.valueOf(request.get("houseId").toString());
        String remark = request.get("remark") != null ? request.get("remark").toString() : null;
        return Result.success(orderService.createIntent(user.getId(), houseId, remark));
    }

    @Operation(summary = "Create appointment order")
    @PostMapping("/appointment")
    public Result<Order> createAppointment(@RequestBody OrderCreateRequest request,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        return Result.success(orderService.createAppointment(request, user.getId()));
    }

    @Operation(summary = "Get order by ID")
    @GetMapping("/{id}")
    public Result<Order> getOrderById(@PathVariable Long id) {
        return Result.success(orderService.getOrderById(id));
    }

    @Operation(summary = "Approve or reject order (landlord)")
    @PutMapping("/{id}/approve")
    public Result<Void> approveOrder(@PathVariable Long id,
                                      @RequestBody Map<String, Object> request,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        boolean approved = Boolean.parseBoolean(request.get("approved").toString());
        orderService.approveOrder(id, approved, user.getId());
        return Result.success();
    }

    @Operation(summary = "Cancel order")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancelOrder(@PathVariable Long id,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        orderService.cancelOrder(id, user.getId());
        return Result.success();
    }

    @Operation(summary = "Complete order")
    @PutMapping("/{id}/complete")
    public Result<Void> completeOrder(@PathVariable Long id) {
        orderService.completeOrder(id);
        return Result.success();
    }

    @Operation(summary = "List my orders as tenant")
    @GetMapping("/my/tenant")
    public Result<PageResult<Order>> listTenantOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = resolveUser(userDetails.getUsername());
        return Result.success(orderService.listTenantOrders(user.getId(), page, size));
    }

    @Operation(summary = "List my orders as landlord")
    @GetMapping("/my/landlord")
    public Result<PageResult<Order>> listLandlordOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = resolveUser(userDetails.getUsername());
        return Result.success(orderService.listLandlordOrders(user.getId(), page, size));
    }

    private User resolveUser(String username) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        return user;
    }
}
