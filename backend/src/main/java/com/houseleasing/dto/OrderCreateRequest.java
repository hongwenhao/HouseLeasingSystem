package com.houseleasing.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 创建订单请求数据传输对象
 *
 * @author HouseLeasingSystem开发团队
 * @description 封装创建租房订单（预约看房）所需的请求参数
 */
@Data
public class OrderCreateRequest {
    /** 目标房源的 ID */
    private Long houseId;
    /** 预约看房的具体时间 */
    private LocalDateTime appointmentTime;
    /** 期望租赁开始日期 */
    private LocalDate startDate;
    /** 期望租赁结束日期 */
    private LocalDate endDate;
    /** 订单备注（如特殊需求） */
    private String remark;
}
