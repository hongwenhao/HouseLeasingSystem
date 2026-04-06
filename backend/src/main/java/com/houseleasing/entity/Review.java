package com.houseleasing.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价实体类
 */
@Data
@TableName("reviews")
public class Review {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 被评价房源 ID（外键 -> houses.id） */
    private Long houseId;
    /** 关联订单 ID（外键 -> orders.id） */
    private Long orderId;
    /** 评价用户（租客）ID（外键 -> users.id） */
    private Long userId;
    /** 评分（1-5） */
    private Integer rating;
    /** 评价内容 */
    private String content;
    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
