package com.houseleasing.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStatusUtilTest {

    @Test
    void isPayableStatus_shouldReturnTrue_forApprovedAndSigned() {
        assertTrue(OrderStatusUtil.isPayableStatus("APPROVED"));
        assertTrue(OrderStatusUtil.isPayableStatus("SIGNED"));
    }

    @Test
    void isPayableStatus_shouldReturnFalse_forOtherStatusesAndNull() {
        assertFalse(OrderStatusUtil.isPayableStatus("PENDING"));
        assertFalse(OrderStatusUtil.isPayableStatus("REJECTED"));
        assertFalse(OrderStatusUtil.isPayableStatus("COMPLETED"));
        assertFalse(OrderStatusUtil.isPayableStatus(null));
    }
}
