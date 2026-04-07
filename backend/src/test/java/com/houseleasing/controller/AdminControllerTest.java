package com.houseleasing.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.houseleasing.common.PageResult;
import com.houseleasing.common.Result;
import com.houseleasing.entity.Contract;
import com.houseleasing.entity.Order;
import com.houseleasing.mapper.ContractMapper;
import com.houseleasing.mapper.HouseMapper;
import com.houseleasing.mapper.OrderMapper;
import com.houseleasing.mapper.UserMapper;
import com.houseleasing.mq.MessageProducer;
import com.houseleasing.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminControllerTest {

    private UserService userService;
    private UserMapper userMapper;
    private HouseMapper houseMapper;
    private OrderMapper orderMapper;
    private ContractMapper contractMapper;
    private MessageProducer messageProducer;
    private AdminController adminController;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        userMapper = mock(UserMapper.class);
        houseMapper = mock(HouseMapper.class);
        orderMapper = mock(OrderMapper.class);
        contractMapper = mock(ContractMapper.class);
        messageProducer = mock(MessageProducer.class);
        adminController = new AdminController(
                userService,
                userMapper,
                houseMapper,
                orderMapper,
                contractMapper,
                messageProducer
        );
    }

    @Test
    void listAllOrders_shouldNotThrow_whenStatusFilterReturnsEmptyPage() {
        Page<Order> emptyPage = new Page<>(1, 100);
        emptyPage.setTotal(0);
        emptyPage.setRecords(List.of());
        when(orderMapper.selectPage(any(Page.class), any())).thenReturn(emptyPage);

        Result<PageResult<Order>> result = assertDoesNotThrow(
                () -> adminController.listAllOrders(1, 100, null, "SIGNED")
        );
        assertNotNull(result);
        assertNotNull(result.getData());
        assertEquals(0, result.getData().getTotal());
        assertNotNull(result.getData().getRecords());
        assertEquals(0, result.getData().getRecords().size());
    }

    @Test
    void listAllContracts_shouldNotThrow_whenStatusFilterReturnsEmptyPage() {
        Page<Contract> emptyPage = new Page<>(1, 100);
        emptyPage.setTotal(0);
        emptyPage.setRecords(List.of());
        when(contractMapper.selectPage(any(Page.class), any())).thenReturn(emptyPage);

        Result<PageResult<Contract>> result = assertDoesNotThrow(
                () -> adminController.listAllContracts(1, 100, null, "FULLY_SIGNED")
        );
        assertNotNull(result);
        assertNotNull(result.getData());
        assertEquals(0, result.getData().getTotal());
        assertNotNull(result.getData().getRecords());
        assertEquals(0, result.getData().getRecords().size());
    }

    @Test
    void cancelOrderByAdmin_shouldCancelRelatedContract_whenLatestContractExists() {
        Order order = new Order();
        order.setId(100L);
        order.setStatus("APPROVED");
        order.setTenantId(10L);
        order.setLandlordId(20L);
        order.setOrderNo("ORD-100");

        Contract latestContract = new Contract();
        latestContract.setId(200L);
        latestContract.setOrderId(100L);
        latestContract.setStatus("PENDING_SIGN");
        latestContract.setTenantId(10L);
        latestContract.setLandlordId(20L);
        latestContract.setContractNo("HT-200");

        when(orderMapper.selectById(100L)).thenReturn(order);
        when(contractMapper.selectOne(any())).thenReturn(latestContract);

        Result<Void> result = assertDoesNotThrow(() -> adminController.cancelOrderByAdmin(100L));

        assertNotNull(result);
        assertEquals("CANCELLED", order.getStatus());
        assertEquals("CANCELLED", latestContract.getStatus());
        verify(orderMapper).updateById(order);
        verify(contractMapper).updateById(latestContract);
        verify(messageProducer, times(2)).sendOrderStatusChange(any(), any());
        verify(messageProducer, times(2)).sendContractStatusChange(any(), any());
    }

    @Test
    void cancelContractByAdmin_shouldCancelRelatedOrder_whenOrderExists() {
        Contract contract = new Contract();
        contract.setId(300L);
        contract.setOrderId(400L);
        contract.setHouseId(500L);
        contract.setStatus("PENDING_SIGN");
        contract.setTenantId(30L);
        contract.setLandlordId(40L);
        contract.setContractNo("HT-300");

        Order order = new Order();
        order.setId(400L);
        order.setStatus("APPROVED");
        order.setTenantId(30L);
        order.setLandlordId(40L);
        order.setOrderNo("ORD-400");

        when(contractMapper.selectById(300L)).thenReturn(contract);
        when(orderMapper.selectById(400L)).thenReturn(order);

        Result<Void> result = assertDoesNotThrow(() -> adminController.cancelContractByAdmin(300L));

        assertNotNull(result);
        assertEquals("CANCELLED", contract.getStatus());
        assertEquals("CANCELLED", order.getStatus());
        verify(contractMapper).updateById(contract);
        verify(orderMapper).updateById(order);
        verify(messageProducer, times(2)).sendOrderStatusChange(any(), any());
        verify(messageProducer, times(2)).sendContractStatusChange(any(), any());
    }
}
