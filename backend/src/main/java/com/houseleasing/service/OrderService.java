package com.houseleasing.service;

import com.houseleasing.common.PageResult;
import com.houseleasing.dto.OrderCreateRequest;
import com.houseleasing.entity.Order;

public interface OrderService {
    Order createIntent(Long tenantId, Long houseId, String remark);
    Order createAppointment(OrderCreateRequest request, Long tenantId);
    void approveOrder(Long orderId, boolean approved, Long landlordId);
    void cancelOrder(Long orderId, Long userId);
    Order getOrderById(Long id);
    PageResult<Order> listTenantOrders(Long tenantId, int page, int size);
    PageResult<Order> listLandlordOrders(Long landlordId, int page, int size);
    void completeOrder(Long orderId);
}
