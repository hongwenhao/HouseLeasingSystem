package com.houseleasing.common.utils;

/**
 * 订单状态工具类
 *
 * <p>用于沉淀跨服务复用的订单状态判定逻辑，避免各处重复硬编码状态值导致口径不一致。</p>
 */
public final class OrderStatusUtil {

    private static final String STATUS_APPROVED = "APPROVED";//已通过审批
    private static final String STATUS_SIGNED = "SIGNED";   //已签约

    private OrderStatusUtil() {
    }

    /**
     * 判断订单是否处于“允许支付”的业务状态。
     * 当前规则：
     * 1) APPROVED：兼容历史流程；
     * 2) SIGNED：当前流程中双方签约后的标准状态。</p>
     *
     * @param status 订单状态
     * @return true 表示允许进入支付前置校验
     */
    public static boolean isPayableStatus(String status) {
        return STATUS_APPROVED.equals(status) || STATUS_SIGNED.equals(status);
    }
}
