package com.houseleasing.controller;

import com.houseleasing.common.PageResult;
import com.houseleasing.common.Result;
import com.houseleasing.common.exception.BusinessException;
import com.houseleasing.dto.OrderCreateRequest;
import com.houseleasing.dto.OrderReviewRequest;
import com.houseleasing.dto.ReviewRecordResponse;
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

/**
 * 订单管理控制器
 *
 * @author hongwenhao
 * @description 提供订单相关的 REST API，所有接口均需要 JWT 认证，
 *              包括创建意向订单、预约订单、审批、取消和查询
 */
@Tag(name = "Order", description = "Order management")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class OrderController {

    private final OrderService orderService;
    private final UserMapper userMapper;

    /**
     * 创建意向订单，租客表达对某房源的租房意向
     *
     * @param request     请求体，包含 houseId 和 remark
     * @param userDetails 当前登录用户信息
     * @return 创建成功的意向订单
     */
    @Operation(summary = "Create intent order")
    @PostMapping("/intent")
    public Result<Order> createIntent(@RequestBody Map<String, Object> request,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        Long houseId = Long.valueOf(request.get("houseId").toString());
        String remark = request.get("remark") != null ? request.get("remark").toString() : null;
        return Result.success(orderService.createIntent(user.getId(), houseId, remark));
    }

    /**
     * 创建预约看房订单
     *
     * @param request     包含预约时间、起止日期等信息的请求对象
     * @param userDetails 当前登录用户信息
     * @return 创建成功的预约订单
     */
    @Operation(summary = "Create appointment order")
    @PostMapping("/appointment")
    public Result<Order> createAppointment(@RequestBody OrderCreateRequest request,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        return Result.success(orderService.createAppointment(request, user.getId()));
    }

    /**
     * 根据订单 ID 查询订单详情
     *
     * @param id 订单 ID
     * @return 订单详情
     */
    @Operation(summary = "Get order by ID")
    @GetMapping("/{id}")
    public Result<Order> getOrderById(@PathVariable Long id) {
        return Result.success(orderService.getOrderById(id));
    }

    /**
     * 房东审批订单（批准或拒绝）
     *
     * @param id          订单 ID
     * @param request     请求体，包含 approved 字段
     * @param userDetails 当前登录用户（必须是该订单的房东）
     * @return 操作成功的响应
     */
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

    /**
     * 取消订单（租客或房东均可操作）
     *
     * @param id          订单 ID
     * @param userDetails 当前登录用户信息
     * @return 操作成功的响应
     */
    @Operation(summary = "Cancel order")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancelOrder(@PathVariable Long id,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        orderService.cancelOrder(id, user.getId());
        return Result.success();
    }

    /**
     * 将订单标记为已完成
     *
     * @param id 订单 ID
     * @return 操作成功的响应
     */
    @Operation(summary = "Complete order")
    @PutMapping("/{id}/complete")
    public Result<Void> completeOrder(@PathVariable Long id) {
        orderService.completeOrder(id);
        return Result.success();
    }

    /**
     * 租客支付订单：
     * 仅当订单已批准且合同双方都签署后才允许支付。
     * 支付成功后，订单状态自动更新为 COMPLETED，支付状态更新为 PAID。
     *
     * @param id          订单 ID
     * @param userDetails 当前登录租客
     * @return 操作成功响应
     */
    @Operation(summary = "Pay order (tenant)")
    @PutMapping("/{id}/pay")
    public Result<Void> payOrder(@PathVariable Long id,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        orderService.payOrder(id, user.getId());
        return Result.success();
    }

    /**
     * 租客退款订单：
     * 仅允许对已支付订单发起退款。
     * 退款成功后，订单状态更新为 CANCELLED，支付状态更新为 REFUNDED。
     *
     * @param id          订单 ID
     * @param userDetails 当前登录租客
     * @return 操作成功响应
     */
    @Operation(summary = "Refund order (tenant)")
    @PutMapping("/{id}/refund")
    public Result<Void> refundOrder(@PathVariable Long id,
                                    @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        orderService.refundOrder(id, user.getId());
        return Result.success();
    }

    /**
     * 租客对已完成订单进行评价
     *
     * @param id 订单 ID
     * @param request 评价请求
     * @param userDetails 当前登录租客
     * @return 操作成功响应
     */
    @Operation(summary = "Review completed order (tenant)")
    @PostMapping("/{id}/review")
    public Result<Void> reviewOrder(@PathVariable Long id,
                                    @RequestBody OrderReviewRequest request,
                                    @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        orderService.reviewOrder(id, user.getId(), request);
        return Result.success();
    }

    /**
     * 查询当前用户作为租客的订单列表
     *
     * @param userDetails 当前登录用户信息
     * @param page        当前页码
     * @param size        每页大小
     * @return 租客订单分页列表
     */
    @Operation(summary = "List my orders as tenant")
    @GetMapping("/my/tenant")
    public Result<PageResult<Order>> listTenantOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = resolveUser(userDetails.getUsername());
        return Result.success(orderService.listTenantOrders(user.getId(), page, size));
    }

    /**
     * 查询当前用户作为房东的订单列表
     *
     * @param userDetails 当前登录用户信息
     * @param page        当前页码
     * @param size        每页大小
     * @return 房东订单分页列表
     */
    @Operation(summary = "List my orders as landlord")
    @GetMapping("/my/landlord")
    public Result<PageResult<Order>> listLandlordOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = resolveUser(userDetails.getUsername());
        return Result.success(orderService.listLandlordOrders(user.getId(), page, size));
    }

    /**
     * 查询当前租客提交过的评价记录（评价管理-租客端）
     *
     * @param userDetails 当前登录用户信息
     * @param page 当前页码
     * @param size 每页大小
     * @return 租客评价记录分页列表
     */
    @Operation(summary = "List my reviews as tenant")
    @GetMapping("/my/reviews/tenant")
    public Result<PageResult<ReviewRecordResponse>> listTenantReviews(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = resolveUser(userDetails.getUsername());
        return Result.success(orderService.listTenantReviewRecords(user.getId(), page, size));
    }

    /**
     * 查询当前房东收到的评价记录（评价管理-房东端）
     *
     * @param userDetails 当前登录用户信息
     * @param page 当前页码
     * @param size 每页大小
     * @return 房东评价记录分页列表
     */
    @Operation(summary = "List my reviews as landlord")
    @GetMapping("/my/reviews/landlord")
    public Result<PageResult<ReviewRecordResponse>> listLandlordReviews(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = resolveUser(userDetails.getUsername());
        return Result.success(orderService.listLandlordReviewRecords(user.getId(), page, size));
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
}
