package com.houseleasing.service;

import com.houseleasing.common.PageResult;
import com.houseleasing.dto.OrderCreateRequest;
import com.houseleasing.dto.OrderReviewRequest;
import com.houseleasing.dto.ReviewRecordResponse;
import com.houseleasing.entity.Order;

/**
 * 订单服务接口
 *
 * @author hongwenhao
 * @description 定义订单相关的业务操作，包括创建意向订单、预约看房订单、审批、取消和查询
 */
public interface OrderService { // 订单主流程抽象：创建、审批、支付、退款、评价、查询

    /**
     * 租客创建意向订单（表达租房意向）
     *
     * @param tenantId 租客用户 ID
     * @param houseId  目标房源 ID
     * @param remark   备注信息
     * @return 创建的意向订单对象
     */
    Order createIntent(Long tenantId, Long houseId, String remark); // 创建意向订单

    /**
     * 租客创建预约看房订单
     *
     * @param request  包含预约信息的请求对象
     * @param tenantId 租客用户 ID
     * @return 创建的预约订单对象
     */
    Order createAppointment(OrderCreateRequest request, Long tenantId); // 创建预约订单

    /**
     * 房东审批订单（批准或拒绝）
     *
     * @param orderId    要审批的订单 ID
     * @param approved   true 表示批准，false 表示拒绝
     * @param landlordId 操作的房东用户 ID（用于权限验证）
     */
    void approveOrder(Long orderId, boolean approved, Long landlordId); // 房东审批订单

    /**
     * 取消订单（租客或房东均可操作）
     *
     * @param orderId 要取消的订单 ID
     * @param userId  操作人用户 ID（用于权限验证）
     */
    void cancelOrder(Long orderId, Long userId); // 取消订单

    /**
     * 根据订单 ID 查询订单详情
     *
     * @param id 订单 ID
     * @return 订单详情对象
     */
    Order getOrderById(Long id); // 查询订单详情

    /**
     * 查询租客的订单列表（分页）
     *
     * @param tenantId 租客用户 ID
     * @param page     当前页码
     * @param size     每页大小
     * @return 该租客的分页订单列表
     */
    PageResult<Order> listTenantOrders(Long tenantId, int page, int size); // 查询租客订单分页

    /**
     * 查询房东的订单列表（分页）
     *
     * @param landlordId 房东用户 ID
     * @param page       当前页码
     * @param size       每页大小
     * @return 该房东的分页订单列表
     */
    PageResult<Order> listLandlordOrders(Long landlordId, int page, int size); // 查询房东订单分页

    /**
     * 将订单标记为已完成
     *
     * @param orderId 要完成的订单 ID
     */
    void completeOrder(Long orderId); // 标记订单完成

    /**
     * 支付订单（租客操作）
     * 仅当订单为 APPROVED（兼容历史）或 SIGNED（双方签约后）且合同双方已签署时允许支付；
     * 支付后订单状态更新为 COMPLETED，支付状态更新为 PAID。
     *
     * @param orderId   订单 ID
     * @param tenantId  当前租客用户 ID（用于权限校验）
     */
    void payOrder(Long orderId, Long tenantId); // 支付订单

    /**
     * 退款订单（租客操作）
     * 仅当订单支付状态为 PAID 时允许退款；
     * 退款后订单状态更新为 CANCELLED，支付状态更新为 REFUNDED。
     *
     * @param orderId   订单 ID
     * @param tenantId  当前租客用户 ID（用于权限校验）
     */
    void refundOrder(Long orderId, Long tenantId); // 退款订单

    /**
     * 租客对已完成订单进行评价，并按评分调整房东信用分
     *
     * @param orderId 订单 ID
     * @param tenantId 当前租客用户 ID
     * @param request 评价请求
     */
    void reviewOrder(Long orderId, Long tenantId, OrderReviewRequest request); // 提交订单评价

    /**
     * 查询当前租客提交的评价记录（分页，按时间倒序）
     *
     * @param tenantId 当前租客 ID
     * @param page 当前页码
     * @param size 每页大小
     * @return 评价记录分页结果
     */
    PageResult<ReviewRecordResponse> listTenantReviewRecords(Long tenantId, int page, int size); // 查询租客提交的评价

    /**
     * 查询当前房东收到的评价记录（分页，按时间倒序）
     *
     * @param landlordId 当前房东 ID
     * @param page 当前页码
     * @param size 每页大小
     * @return 评价记录分页结果
     */
    PageResult<ReviewRecordResponse> listLandlordReviewRecords(Long landlordId, int page, int size); // 查询房东收到的评价
}
