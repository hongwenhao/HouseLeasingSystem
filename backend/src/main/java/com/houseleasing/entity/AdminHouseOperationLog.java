package com.houseleasing.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员房源操作日志实体
 *
 * @author HouseLeasingSystem开发团队
 * @description 对应 admin_house_operation_logs 表，用于记录管理员对房源执行上架/下架等管理操作，
 *              便于在房源管理详情页以时间线方式展示“谁在何时做了什么”
 */
@Data
@TableName("admin_house_operation_logs")
public class AdminHouseOperationLog {
    /** 日志主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 被操作的房源 ID */
    private Long houseId;
    /** 操作动作（如：上架、下架） */
    private String action;
    /** 操作人用户 ID（管理员） */
    private Long operatorId;
    /** 操作人用户名（冗余保存，便于展示） */
    private String operatorName;
    /** 操作备注（可选） */
    private String remark;
    /** 记录创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
