package com.houseleasing.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.houseleasing.activiti.WorkflowService;
import com.houseleasing.common.security.IdCardCryptoService;
import com.houseleasing.entity.Contract;
import com.houseleasing.entity.House;
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
    private IdCardCryptoService idCardCryptoService;
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
        idCardCryptoService = mock(IdCardCryptoService.class);
        contractService = new ContractServiceImpl(
                workflowService,
                contractMapper,
                orderMapper,
                houseMapper,
                userMapper,
                contractRiskService,
                messageProducer,
                new ObjectMapper(),
                idCardCryptoService
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

        contractService.signContract(1L, 10L, "TENANT");

        assertEquals("FULLY_SIGNED", contract.getStatus());
        verify(orderMapper, times(1)).markOrderSignedIfApproved(100L);
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

        contractService.signContract(1L, 20L, "LANDLORD");

        assertEquals("FULLY_SIGNED", contract.getStatus());
        verify(orderMapper, times(1)).markOrderSignedIfApproved(100L);
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
        verify(orderMapper, never()).markOrderSignedIfApproved(any());
        verify(contractMapper, times(1)).updateById(contract);
    }

    @Test
    void cancelContract_shouldAlsoCancelRelatedOrder_whenUserCancelsContract() {
        Contract contract = new Contract();
        contract.setId(8L);
        contract.setOrderId(88L);
        contract.setHouseId(188L);
        contract.setTenantId(10L);
        contract.setLandlordId(20L);
        contract.setStatus("PENDING_SIGN");
        when(contractMapper.selectById(8L)).thenReturn(contract);

        Order order = new Order();
        order.setId(88L);
        order.setStatus("SIGNED");
        when(orderMapper.selectById(88L)).thenReturn(order);

        House house = new House();
        house.setId(188L);
        house.setStatus("OFFLINE");
        when(houseMapper.selectById(188L)).thenReturn(house);

        assertDoesNotThrow(() -> contractService.cancelContract(8L, 10L));

        assertEquals("CANCELLED", contract.getStatus());
        assertEquals("CANCELLED", order.getStatus());
        assertEquals("ONLINE", house.getStatus());
        verify(contractMapper, times(1)).updateById(contract);
        verify(orderMapper, times(1)).updateById(order);
        verify(houseMapper, times(1)).updateById(house);
    }
}
