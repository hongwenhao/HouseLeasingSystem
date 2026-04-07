package com.houseleasing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单实体类
 *
 * @author hongwenhao
 * @description 对应数据库 orders 表，存储租房订单信息，
 *              支持意向订单和预约看房订单两种类型
 */
@Data
@TableName("orders")
public class Order {
    /** 订单主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联的房源 ID */
    private Long houseId;
    /** 租客用户 ID */
    private Long tenantId;
    /** 房东用户 ID */
    private Long landlordId;
    /** 订单号（系统生成的唯一编号） */
    private String orderNo;
    /** 订单状态：PENDING（待处理）、APPROVED（房东已确认）、SIGNED（双方已签约）、REJECTED（已拒绝）、CANCELLED（已取消）、COMPLETED（已完成） */
    private String status = "PENDING";
    /** 预约看房时间 */
    private LocalDateTime appointmentTime;
    /** 租赁开始日期 */
    private LocalDate startDate;
    /** 租赁结束日期 */
    private LocalDate endDate;
    /** 月租金（元） */
    private BigDecimal monthlyRent;
    /** 押金金额（元） */
    private BigDecimal deposit;
    /** 订单总金额（元） */
    private BigDecimal totalAmount;
    /** 支付状态：UNPAID（未支付）、PAID（已支付）、REFUNDED（已退款） */
    private String paymentStatus = "UNPAID";
    /** 关联合同状态（非数据库字段，用于前端判断是否可支付） */
    @TableField(exist = false)
    private String contractStatus;
    /** 是否达到可支付条件（非数据库字段：仅当合同双方都签署时为 true） */
    @TableField(exist = false)
    private Boolean canPay;
    /** 关联的最新合同 ID（非数据库字段，用于前端快速跳转合同详情） */
    @TableField(exist = false)
    private Long contractId;
    /** 关联的最新合同编号（非数据库字段，用于前端展示） */
    @TableField(exist = false)
    private String contractNo;
    /** 订单备注信息 */
    private String remark;
    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /** 更新时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    /** 关联的房源信息（非数据库字段，通过查询关联填充） */
    @TableField(exist = false)
    private House house;
    /** 租客用户信息（非数据库字段，通过查询关联填充） */
    @TableField(exist = false)
    private User tenant;
    /** 房东用户信息（非数据库字段，通过查询关联填充） */
    @TableField(exist = false)
    private User landlord;
    /** 当前租客是否已对该订单完成评价（非数据库字段，用于前端控制“去评价”入口展示） */
    @TableField(exist = false)
    private Boolean reviewed;
}
