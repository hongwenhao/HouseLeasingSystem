package com.houseleasing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户行为记录实体类
 *
 * @author hongwenhao
 * @description 对应数据库 user_behaviors 表，记录用户与房源的交互行为，
 *              用于推荐系统的协同过滤算法数据来源
 */
@Data
@TableName("user_behaviors")
public class UserBehavior {
    /** 行为记录主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 执行行为的用户 ID */
    private Long userId;
    /** 被操作的房源 ID */
    private Long houseId;
    /** 行为类型：VIEW（浏览）、COLLECT（收藏）、ORDER（下单） */
    private String behaviorType;
    /** 行为权重分值（不同行为类型对应不同分值） */
    private java.math.BigDecimal score;
    /** 行为发生时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
