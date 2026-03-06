package com.houseleasing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.houseleasing.common.PageResult;
import com.houseleasing.common.exception.BusinessException;
import com.houseleasing.dto.OrderCreateRequest;
import com.houseleasing.entity.House;
import com.houseleasing.entity.Order;
import com.houseleasing.mapper.HouseMapper;
import com.houseleasing.mapper.OrderMapper;
import com.houseleasing.mq.MessageProducer;
import com.houseleasing.service.MessageService;
import com.houseleasing.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final HouseMapper houseMapper;
    private final MessageProducer messageProducer;
    private final MessageService messageService;

    @Override
    @Transactional
    public Order createIntent(Long tenantId, Long houseId, String remark) {
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            throw new BusinessException(404, "House not found");
        }
        Order order = new Order();
        order.setHouseId(houseId);
        order.setTenantId(tenantId);
        order.setLandlordId(house.getOwnerId());
        order.setOrderNo(generateOrderNo("INT"));
        order.setOrderType("INTENT");
        order.setStatus("PENDING");
        order.setMonthlyRent(house.getPrice());
        order.setDeposit(house.getDeposit());
        order.setRemark(remark);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);
        messageProducer.sendOrderStatusChange(house.getOwnerId(), "新的意向订单");
        return order;
    }

    @Override
    @Transactional
    public Order createAppointment(OrderCreateRequest request, Long tenantId) {
        House house = houseMapper.selectById(request.getHouseId());
        if (house == null) {
            throw new BusinessException(404, "House not found");
        }
        Order order = new Order();
        order.setHouseId(request.getHouseId());
        order.setTenantId(tenantId);
        order.setLandlordId(house.getOwnerId());
        order.setOrderNo(generateOrderNo("APT"));
        order.setOrderType(request.getOrderType() != null ? request.getOrderType() : "APPOINTMENT");
        order.setStatus("PENDING");
        order.setAppointmentTime(request.getAppointmentTime());
        order.setStartDate(request.getStartDate());
        order.setEndDate(request.getEndDate());
        order.setMonthlyRent(house.getPrice());
        order.setDeposit(house.getDeposit());
        order.setRemark(request.getRemark());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);
        messageProducer.sendAppointmentConfirmation(tenantId, house.getTitle());
        messageProducer.sendOrderStatusChange(house.getOwnerId(), "新的预约订单");
        return order;
    }

    @Override
    @Transactional
    public void approveOrder(Long orderId, boolean approved, Long landlordId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "Order not found");
        }
        if (!order.getLandlordId().equals(landlordId)) {
            throw new BusinessException(403, "Not authorized");
        }
        order.setStatus(approved ? "APPROVED" : "REJECTED");
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        messageProducer.sendOrderStatusChange(order.getTenantId(),
                approved ? "您的订单已被批准" : "您的订单已被拒绝");
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "Order not found");
        }
        if (!order.getTenantId().equals(userId) && !order.getLandlordId().equals(userId)) {
            throw new BusinessException(403, "Not authorized");
        }
        order.setStatus("CANCELLED");
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Override
    public Order getOrderById(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "Order not found");
        }
        return order;
    }

    @Override
    public PageResult<Order> listTenantOrders(Long tenantId, int page, int size) {
        Page<Order> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getTenantId, tenantId);
        wrapper.orderByDesc(Order::getCreateTime);
        Page<Order> result = orderMapper.selectPage(pageObj, wrapper);
        return PageResult.of(result.getTotal(), result.getRecords(), page, size);
    }

    @Override
    public PageResult<Order> listLandlordOrders(Long landlordId, int page, int size) {
        Page<Order> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getLandlordId, landlordId);
        wrapper.orderByDesc(Order::getCreateTime);
        Page<Order> result = orderMapper.selectPage(pageObj, wrapper);
        return PageResult.of(result.getTotal(), result.getRecords(), page, size);
    }

    @Override
    @Transactional
    public void completeOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "Order not found");
        }
        order.setStatus("COMPLETED");
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    private String generateOrderNo(String prefix) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return prefix + timestamp + random;
    }
}
