package com.houseleasing.dto;

import lombok.Data;

/**
 * 订单评价请求对象
 */
@Data
public class OrderReviewRequest {
    /** 星级评分（1-5） */
    private Integer rating;
    /** 评价内容 */
    private String content;
}
