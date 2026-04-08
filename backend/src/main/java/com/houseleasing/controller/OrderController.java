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
@RequestMapping("/api/orders") // 订单接口统一前缀
@RequiredArgsConstructor // 自动注入依赖
@SecurityRequirement(name = "Bearer Authentication")
public class OrderController { // 负责意向单、预约单、审批、支付、评价等订单流程

    private final OrderService orderService; // 订单业务服务
    private final UserMapper userMapper; // 用户查询组件

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
                                       @AuthenticationPrincipal UserDetails userDetails) { // 创建“我想租这个房”的意向单
        User user = resolveUser(userDetails.getUsername()); // 解析当前登录用户
        Long houseId = Long.valueOf(request.get("houseId").toString()); // 取出要租的房源ID
        String remark = request.get("remark") != null ? request.get("remark").toString() : null; // 取出备注（可为空）
        return Result.success(orderService.createIntent(user.getId(), houseId, remark)); // 创建并返回意向订单
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
                                             @AuthenticationPrincipal UserDetails userDetails) { // 创建预约看房订单
        User user = resolveUser(userDetails.getUsername()); // 获取当前用户
        return Result.success(orderService.createAppointment(request, user.getId())); // 提交预约参数并返回订单
    }

    /**
     * 根据订单 ID 查询订单详情
     *
     * @param id 订单 ID
     * @return 订单详情
     */
    @Operation(summary = "Get order by ID")
    @GetMapping("/{id}")
    public Result<Order> getOrderById(@PathVariable Long id) { // 查询单个订单详情
        return Result.success(orderService.getOrderById(id)); // 按订单ID返回详情
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
                                      @AuthenticationPrincipal UserDetails userDetails) { // 房东对订单做“同意/拒绝”
        User user = resolveUser(userDetails.getUsername()); // 获取当前房东
        boolean approved = Boolean.parseBoolean(request.get("approved").toString()); // 读取是否同意
        orderService.approveOrder(id, approved, user.getId()); // 执行审批逻辑
        return Result.success(); // 返回审批成功
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
                                     @AuthenticationPrincipal UserDetails userDetails) { // 取消订单
        User user = resolveUser(userDetails.getUsername()); // 解析当前操作用户
        orderService.cancelOrder(id, user.getId()); // 执行业务取消
        return Result.success(); // 返回取消成功
    }

    /**
     * 将订单标记为已完成
     *
     * @param id 订单 ID
     * @return 操作成功的响应
     */
    @Operation(summary = "Complete order")
    @PutMapping("/{id}/complete")
    public Result<Void> completeOrder(@PathVariable Long id) { // 把订单标记为“已完成”
        orderService.completeOrder(id); // 执行完成动作
        return Result.success(); // 返回成功
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
                                 @AuthenticationPrincipal UserDetails userDetails) { // 租客支付订单
        User user = resolveUser(userDetails.getUsername()); // 获取当前租客
        orderService.payOrder(id, user.getId()); // 执行支付与状态变更
        return Result.success(); // 返回支付成功
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
                                    @AuthenticationPrincipal UserDetails userDetails) { // 租客发起退款
        User user = resolveUser(userDetails.getUsername()); // 获取当前租客
        orderService.refundOrder(id, user.getId()); // 执行退款与状态变更
        return Result.success(); // 返回退款成功
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
                                     @AuthenticationPrincipal UserDetails userDetails) { // 租客提交评价
        User user = resolveUser(userDetails.getUsername()); // 获取当前租客
        orderService.reviewOrder(id, user.getId(), request); // 保存评分和评价内容
        return Result.success(); // 返回评价成功
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
            @RequestParam(defaultValue = "10") int size) { // 查询“我作为租客”的订单
        User user = resolveUser(userDetails.getUsername()); // 获取当前用户
        return Result.success(orderService.listTenantOrders(user.getId(), page, size)); // 返回租客订单分页
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
            @RequestParam(defaultValue = "10") int size) { // 查询“我作为房东”的订单
        User user = resolveUser(userDetails.getUsername()); // 获取当前用户
        return Result.success(orderService.listLandlordOrders(user.getId(), page, size)); // 返回房东订单分页
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
            @RequestParam(defaultValue = "10") int size) { // 查询“我写过的评价”
        User user = resolveUser(userDetails.getUsername()); // 解析当前用户
        return Result.success(orderService.listTenantReviewRecords(user.getId(), page, size)); // 返回租客评价分页
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
            @RequestParam(defaultValue = "10") int size) { // 查询“我收到的评价”
        User user = resolveUser(userDetails.getUsername()); // 解析当前用户
        return Result.success(orderService.listLandlordReviewRecords(user.getId(), page, size)); // 返回房东评价分页
    }

    /**
     * 根据用户名解析用户信息
     *
     * @param username 用户名
     * @return 对应的用户实体
     */
    private User resolveUser(String username) { // 辅助方法：按用户名找用户
        User user = userMapper.selectByUsername(username); // 数据库查询
        if (user == null) { // 无此用户
            throw new BusinessException(404, "用户不存在"); // 抛出明确错误
        }
        return user; // 返回用户实体
    }
}
