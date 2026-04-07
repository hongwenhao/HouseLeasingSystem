package com.houseleasing.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 生成合同请求数据传输对象
 *
 * @author hongwenhao
 * @description 封装根据订单生成租赁合同所需的请求参数
 */
@Data
public class ContractGenerateRequest {
    /** 基于该订单生成合同的订单 ID */
    private Long orderId;
    /** 房东在生成合同时填写的租赁起始日期 */
    private LocalDate startDate;
    /** 房东在生成合同时填写的租赁终止日期 */
    private LocalDate endDate;
    /** 补充条款内容（可选，追加到合同末尾） */
    private String additionalClauses;
}
