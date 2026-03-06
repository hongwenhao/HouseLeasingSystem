package com.houseleasing.dto;

import lombok.Data;

/**
 * 生成合同请求数据传输对象
 *
 * @author HouseLeasingSystem开发团队
 * @description 封装根据订单生成租赁合同所需的请求参数
 */
@Data
public class ContractGenerateRequest {
    /** 基于该订单生成合同的订单 ID */
    private Long orderId;
    /** 补充条款内容（可选，追加到合同末尾） */
    private String additionalClauses;
}
