package com.houseleasing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_behaviors")
public class UserBehavior {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long houseId;
    private String behaviorType;
    private java.math.BigDecimal score;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
