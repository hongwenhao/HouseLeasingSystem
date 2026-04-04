package com.houseleasing.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价记录视图对象（用于租客/房东评价管理列表展示）
 */
@Data
public class ReviewRecordResponse {
    /** 评价 ID */
    private Long id;
    /** 关联订单 ID */
    private Long orderId;
    /** 关联房源 ID */
    private Long houseId;
    /** 房源标题（展示用） */
    private String houseTitle;
    /** 租客用户 ID */
    private Long tenantId;
    /** 租客名称（展示用） */
    private String tenantName;
    /** 房东用户 ID */
    private Long landlordId;
    /** 房东名称（展示用） */
    private String landlordName;
    /** 评分（1~5） */
    private Integer rating;
    /** 评价内容 */
    private String content;
    /** 创建时间 */
    private LocalDateTime createTime;
}
