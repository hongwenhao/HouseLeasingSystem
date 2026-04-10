package com.houseleasing.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 创建订单请求数据传输对象
 *
 * @author hongwenhao
 * @description 封装创建租房订单（预约看房）所需的请求参数
 */
@Data
public class OrderCreateRequest {
    /** 目标房源的 ID */
    private Long houseId;
    /**
     * 预约看房的具体时间。
     * <p>
     * 前端当前以字符串格式 {@code yyyy-MM-dd HH:mm:ss} 提交（例如：2026-04-10 17:38:31），
     * 默认 LocalDateTime 反序列化期望 ISO-8601（含 'T'，如 2026-04-10T17:38:31），
     * 两者不一致会导致 Jackson 抛出 HttpMessageNotReadableException 并返回 500。
     * </p>
     * <p>
     * 这里显式声明格式后，后端即可稳定兼容前端提交格式，同时保持响应中的时间格式一致，
     * 避免“预约看房”接口因时间解析失败而中断。
     * </p>
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime appointmentTime;
    /** 期望租赁开始日期 */
    private LocalDate startDate;
    /** 期望租赁结束日期 */
    private LocalDate endDate;
    /** 订单备注（如特殊需求） */
    private String remark;
}
