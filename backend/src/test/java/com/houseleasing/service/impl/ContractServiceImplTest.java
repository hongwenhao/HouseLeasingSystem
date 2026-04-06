package com.houseleasing.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.houseleasing.activiti.WorkflowService;
import com.houseleasing.entity.Contract;
import com.houseleasing.entity.Order;
import com.houseleasing.mapper.ContractMapper;
import com.houseleasing.mapper.HouseMapper;
import com.houseleasing.mapper.OrderMapper;
import com.houseleasing.mapper.UserMapper;
import com.houseleasing.mq.MessageProducer;
import com.houseleasing.service.ContractRiskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractServiceImplTest {

    private WorkflowService workflowService;
    private ContractMapper contractMapper;
    private OrderMapper orderMapper;
    private HouseMapper houseMapper;
    private UserMapper userMapper;
    private ContractRiskService contractRiskService;
    private MessageProducer messageProducer;
    private ContractServiceImpl contractService;

    @BeforeEach
    void setUp() {
        workflowService = mock(WorkflowService.class);
        contractMapper = mock(ContractMapper.class);
        orderMapper = mock(OrderMapper.class);
        houseMapper = mock(HouseMapper.class);
        userMapper = mock(UserMapper.class);
        contractRiskService = mock(ContractRiskService.class);
        messageProducer = mock(MessageProducer.class);
        contractService = new ContractServiceImpl(
                workflowService,
                contractMapper,
                orderMapper,
                houseMapper,
                userMapper,
                contractRiskService,
                messageProducer,
                new ObjectMapper()
        );
    }

    @Test
    void signContract_shouldUpdateOrderToSigned_whenBothPartiesSignedAndOrderApproved() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setOrderId(100L);
        contract.setTenantId(10L);
        contract.setLandlordId(20L);
        contract.setTenantSigned(false);
        contract.setLandlordSigned(true);
        contract.setStatus("LANDLORD_SIGNED");
        when(contractMapper.selectById(1L)).thenReturn(contract);

        Order order = new Order();
        order.setId(100L);
        order.setStatus("APPROVED");
        when(orderMapper.selectById(100L)).thenReturn(order);

        contractService.signContract(1L, 10L, "TENANT");

        assertEquals("FULLY_SIGNED", contract.getStatus());
        assertEquals("SIGNED", order.getStatus());
        verify(orderMapper, times(1)).updateById(order);
        verify(contractMapper, times(1)).updateById(contract);
    }

    @Test
    void signContract_shouldKeepOrderStatus_whenBothPartiesSignedButOrderNotApproved() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setOrderId(100L);
        contract.setTenantId(10L);
        contract.setLandlordId(20L);
        contract.setTenantSigned(true);
        contract.setLandlordSigned(false);
        contract.setStatus("TENANT_SIGNED");
        when(contractMapper.selectById(1L)).thenReturn(contract);

        Order order = new Order();
        order.setId(100L);
        order.setStatus("COMPLETED");
        when(orderMapper.selectById(100L)).thenReturn(order);

        contractService.signContract(1L, 20L, "LANDLORD");

        assertEquals("FULLY_SIGNED", contract.getStatus());
        assertEquals("COMPLETED", order.getStatus());
        verify(orderMapper, never()).updateById(any(Order.class));
        verify(contractMapper, times(1)).updateById(contract);
    }

    @Test
    void signContract_shouldNotThrow_whenBothPartiesSignedAndContractHasNoOrder() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setOrderId(null);
        contract.setTenantId(10L);
        contract.setLandlordId(20L);
        contract.setTenantSigned(false);
        contract.setLandlordSigned(true);
        contract.setStatus("LANDLORD_SIGNED");
        when(contractMapper.selectById(1L)).thenReturn(contract);

        assertDoesNotThrow(() -> contractService.signContract(1L, 10L, "TENANT"));

        assertEquals("FULLY_SIGNED", contract.getStatus());
        verify(orderMapper, never()).selectById(any());
        verify(orderMapper, never()).updateById(any(Order.class));
        verify(contractMapper, times(1)).updateById(contract);
    }
}

