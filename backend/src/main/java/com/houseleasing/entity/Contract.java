package com.houseleasing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 租赁合同实体类
 *
 * @author hongwenhao
 * @description 对应数据库 contracts 表，存储房屋租赁合同的完整信息，
 *              包括合同内容、双方签署状态、风险评估结果等
 */
@Data
@TableName("contracts")
public class Contract {
    /** 合同主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 合同编号（系统生成的唯一编号） */
    private String contractNo;
    /** 关联的订单 ID */
    private Long orderId;
    /** 关联的房源 ID */
    private Long houseId;
    /** 租客用户 ID */
    private Long tenantId;
    /** 房东用户 ID */
    private Long landlordId;
    /** 租客用户信息（非数据库字段，通过查询关联填充） */
    @TableField(exist = false)
    private User tenant;
    /** 房东用户信息（非数据库字段，通过查询关联填充） */
    @TableField(exist = false)
    private User landlord;
    /** 关联合同房源信息（非数据库字段，通过查询关联填充） */
    @TableField(exist = false)
    private House house;
    /** 关联订单编号（非数据库字段，用于前端展示“查看对应订单”场景） */
    @TableField(exist = false)
    private String orderNo;
    /** 合同正文内容（LONGVARCHAR 类型存储大文本） */
    @TableField(value = "content", jdbcType = org.apache.ibatis.type.JdbcType.LONGVARCHAR)
    private String content;
    /**
     * 合同状态：
     * DRAFT（草稿）→ PENDING_SIGN（待签署）→ TENANT_SIGNED（租客已签）/LANDLORD_SIGNED（房东已签）
     * → FULLY_SIGNED（双方已签）或 CANCELLED（已取消）
     */
    private String status = "DRAFT";
    /** 风险等级：LOW（低风险）、MEDIUM（中风险）、HIGH（高风险） */
    private String riskLevel;
    /** 风险项列表（JSON 格式存储） */
    private String riskItems;
    /** 租客是否已签署 */
    private Boolean tenantSigned = false;
    /** 房东是否已签署 */
    private Boolean landlordSigned = false;
    /** 租客签署时间 */
    private LocalDateTime tenantSignTime;
    /** 房东签署时间 */
    private LocalDateTime landlordSignTime;
    /** 租赁开始日期 */
    private LocalDate startDate;
    /** 租赁结束日期 */
    private LocalDate endDate;
    /** 月租金（元） */
    private BigDecimal monthlyRent;
    /** 押金金额（元） */
    private BigDecimal deposit;
    /** PDF 文件存储路径 */
    private String pdfPath;
    /** 工作流实例 ID（用于签署流程追踪） */
    private String workflowInstanceId;
    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /** 双方完成签署的时间 */
    private LocalDateTime signTime;
    /** 更新时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
