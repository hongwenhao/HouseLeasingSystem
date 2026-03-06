package com.houseleasing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("contracts")
public class Contract {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String contractNo;
    private Long orderId;
    private Long houseId;
    private Long tenantId;
    private Long landlordId;
    @TableField(value = "content", jdbcType = org.apache.ibatis.type.JdbcType.LONGVARCHAR)
    private String content;
    private String status = "DRAFT";
    private String riskLevel;
    private String riskItems;
    private Boolean tenantSigned = false;
    private Boolean landlordSigned = false;
    private LocalDateTime tenantSignTime;
    private LocalDateTime landlordSignTime;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal monthlyRent;
    private BigDecimal deposit;
    private String pdfPath;
    private String workflowInstanceId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private LocalDateTime signTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
