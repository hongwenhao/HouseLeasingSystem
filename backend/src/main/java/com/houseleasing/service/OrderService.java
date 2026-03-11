package com.houseleasing.service;

import com.houseleasing.common.PageResult;
import com.houseleasing.dto.OrderCreateRequest;
import com.houseleasing.entity.Order;

/**
 * 订单服务接口
 *
 * @author HouseLeasingSystem开发团队
 * @description 定义订单相关的业务操作，包括创建意向订单、预约看房订单、审批、取消和查询
 */
public interface OrderService {

    /**
     * 租客创建意向订单（表达租房意向）
     *
     * @param tenantId 租客用户 ID
     * @param houseId  目标房源 ID
     * @param remark   备注信息
     * @return 创建的意向订单对象
     */
    Order createIntent(Long tenantId, Long houseId, String remark);

    /**
     * 租客创建预约看房订单
     *
     * @param request  包含预约信息的请求对象
     * @param tenantId 租客用户 ID
     * @return 创建的预约订单对象
     */
    Order createAppointment(OrderCreateRequest request, Long tenantId);

    /**
     * 房东审批订单（批准或拒绝）
     *
     * @param orderId    要审批的订单 ID
     * @param approved   true 表示批准，false 表示拒绝
     * @param landlordId 操作的房东用户 ID（用于权限验证）
     */
    void approveOrder(Long orderId, boolean approved, Long landlordId);

    /**
     * 取消订单（租客或房东均可操作）
     *
     * @param orderId 要取消的订单 ID
     * @param userId  操作人用户 ID（用于权限验证）
     */
    void cancelOrder(Long orderId, Long userId);

    /**
     * 根据订单 ID 查询订单详情
     *
     * @param id 订单 ID
     * @return 订单详情对象
     */
    Order getOrderById(Long id);

    /**
     * 查询租客的订单列表（分页）
     *
     * @param tenantId 租客用户 ID
     * @param page     当前页码
     * @param size     每页大小
     * @return 该租客的分页订单列表
     */
    PageResult<Order> listTenantOrders(Long tenantId, int page, int size);

    /**
     * 查询房东的订单列表（分页）
     *
     * @param landlordId 房东用户 ID
     * @param page       当前页码
     * @param size       每页大小
     * @return 该房东的分页订单列表
     */
    PageResult<Order> listLandlordOrders(Long landlordId, int page, int size);

    /**
     * 将订单标记为已完成
     *
     * @param orderId 要完成的订单 ID
     */
    void completeOrder(Long orderId);
}
