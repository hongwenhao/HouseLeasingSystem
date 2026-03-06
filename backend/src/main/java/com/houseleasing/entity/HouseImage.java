package com.houseleasing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("house_images")
public class HouseImage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long houseId;
    private String imageUrl;
    private Integer sort;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
