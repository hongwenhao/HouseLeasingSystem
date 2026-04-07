package com.houseleasing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内消息实体类
 *
 * @author HouseLeasingSystem开发团队
 * @description 对应数据库 messages 表，存储系统发送给用户的站内通知消息，
 *              支持预约通知、合同通知、订单通知等多种类型
 */
@Data
@TableName("messages")
public class Message {
    /** 消息主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 消息接收用户的 ID */
    private Long userId;
    /** 消息标题 */
    private String title;
    /** 消息正文内容 */
    private String content;
    /** 消息类型：APPOINTMENT（预约）、CONTRACT（合同）、ORDER（订单）、REVIEW（评价）、SYSTEM（系统） */
    private String type;
    /** 是否已读，默认未读 */
    private Boolean isRead = false;
    /** 关联业务对象的 ID（如订单 ID、合同 ID等） */
    private Long relatedId;
    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
