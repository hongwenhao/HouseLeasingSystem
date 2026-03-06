package com.houseleasing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("houses")
public class House {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String address;
    private BigDecimal area;
    private String city;
    private String district;
    private BigDecimal price;
    private BigDecimal deposit;
    private String houseType;
    private String ownerType;
    private String status = "PENDING";
    private BigDecimal waterFee;
    private BigDecimal electricFee;
    private BigDecimal gasFee;
    private BigDecimal propertyFee;
    private BigDecimal internetFee;
    private String waterFeeType;
    private String electricFeeType;
    private String gasFeeType;
    private Integer rooms;
    private Integer halls;
    private Integer bathrooms;
    private Integer floor;
    private Integer totalFloor;
    private String decoration;
    private String images;
    private Long ownerId;
    private Integer viewCount = 0;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
