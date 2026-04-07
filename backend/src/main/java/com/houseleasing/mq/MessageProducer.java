package com.houseleasing.mq;

import com.houseleasing.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 消息生产者
 *
 * @author hongwenhao
 * @description 负责向 RabbitMQ 发送各类业务通知消息。
 *              当 RabbitMQ 不可用时，自动降级为直接写入数据库，保证消息可靠性
 */
@Slf4j
@Service
public class MessageProducer {

    /** RabbitMQ 模板，用于发送消息到队列（可选注入，不可用时降级） */
    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    /** 消息服务，RabbitMQ 不可用时直接调用此服务保存消息 */
    @Autowired
    private MessageService messageService;

    /**
     * 发送预约确认通知消息
     * 通知租客的预约申请已提交，等待房东确认
     *
     * @param userId     接收通知的用户 ID（租客）
     * @param houseTitle 预约的房源标题
     * @param relatedId  关联的订单 ID，前端可据此跳转到订单详情页
     */
    public void sendAppointmentConfirmation(Long userId, String houseTitle, Long relatedId) {
        String content = "您的预约申请已提交，房源：" + houseTitle + "，请等待房东确认。";
        try {
            if (rabbitTemplate != null) {
                // RabbitMQ 可用时，发送消息到队列异步处理
                rabbitTemplate.convertAndSend("house.exchange", "appointment.confirm",
                        buildMessage(userId, "预约确认", content, relatedId));
                log.debug("Sent appointment confirmation to queue for user {}", userId);
            } else {
                // RabbitMQ 不可用，直接保存消息到数据库
                saveMessageDirectly(userId, "预约确认", content, "APPOINTMENT", relatedId);
            }
        } catch (Exception e) {
            log.warn("RabbitMQ unavailable, saving message directly: {}", e.getMessage());
            saveMessageDirectly(userId, "预约确认", content, "APPOINTMENT", relatedId);
        }
    }

    /**
     * 发送合同状态变更通知消息
     * 通知用户合同的最新状态（如已签署、已取消等）
     *
     * @param userId         接收通知的用户 ID
     * @param contractStatus 合同状态描述
     * @param relatedId      关联的合同 ID，前端可据此跳转到合同详情页
     */
    public void sendContractStatusChange(Long userId, String contractStatus, Long relatedId) {
        String content = "合同状态更新：" + contractStatus;
        try {
            if (rabbitTemplate != null) {
                // RabbitMQ可用：发送到队列异步处理
                rabbitTemplate.convertAndSend("house.exchange", "contract.status",
                        buildMessage(userId, "合同状态通知", content, relatedId));
                log.debug("已把该用户的合约状态变更请求放入消息队列，等待处理 {}", userId);
            } else {
                saveMessageDirectly(userId, "合同状态通知", content, "CONTRACT", relatedId);
            }
        } catch (Exception e) {
            log.warn("由于RabbitMQ 服务不可用，系统已将消息暂存至本地（或数据库），等待后续处理: {}", e.getMessage());
            saveMessageDirectly(userId, "合同状态通知", content, "CONTRACT", relatedId);
        }
    }

    /**
     * 发送订单状态变更通知消息
     * 通知用户订单的最新状态（如已批准、已拒绝等）
     *
     * @param userId      接收通知的用户 ID
     * @param orderStatus 订单状态描述
     * @param relatedId   关联的订单 ID，前端可据此跳转到订单详情页
     */
    public void sendOrderStatusChange(Long userId, String orderStatus, Long relatedId) {
        String content = "订单状态更新：" + orderStatus;
        try {
            if (rabbitTemplate != null) {
                rabbitTemplate.convertAndSend("house.exchange", "order.status",
                        buildMessage(userId, "订单状态通知", content, relatedId));
                log.debug("Sent order status change to queue for user {}", userId);
            } else {
                saveMessageDirectly(userId, "订单状态通知", content, "ORDER", relatedId);
            }
        } catch (Exception e) {
            log.warn("RabbitMQ unavailable, saving message directly: {}", e.getMessage());
            saveMessageDirectly(userId, "订单状态通知", content, "ORDER", relatedId);
        }
    }

    /**
     * 发送登录提醒消息
     * 登录属于关键操作，需要在消息中心保留可追溯记录。
     * 登录事件无关联业务对象，relatedId 为 null
     *
     * @param userId  接收通知的用户 ID
     * @param content 登录提醒正文
     */
    public void sendLoginNotification(Long userId, String content) {
        try {
            if (rabbitTemplate != null) {
                // 登录提醒使用独立路由键 login.*，便于按消息类型解耦与监控。
                rabbitTemplate.convertAndSend("house.exchange", "login.notice",
                        buildMessage(userId, "登录提醒", content, null));
                log.debug("Sent login notification to queue for user {}", userId);
            } else {
                saveMessageDirectly(userId, "登录提醒", content, "SYSTEM", null);
            }
        } catch (Exception e) {
            log.warn("RabbitMQ unavailable, saving login message directly: {}", e.getMessage());
            saveMessageDirectly(userId, "登录提醒", content, "SYSTEM", null);
        }
    }

    /**
     * 发送管理员房源管理通知（上架/下架/恢复等）
     *
     * @param userId      接收通知的用户 ID（通常为房东，也可扩展给租客）
     * @param actionLabel 管理动作标签（如：上架、下架）
     * @param content     具体通知正文
     * @param relatedId   关联的房源 ID，前端可据此跳转到房源详情页
     */
    public void sendAdminHouseManagementNotification(Long userId, String actionLabel, String content, Long relatedId) {
        String title = "房源" + actionLabel + "通知";
        try {
            if (rabbitTemplate != null) {
                rabbitTemplate.convertAndSend("house.exchange", "house.admin.manage",
                        buildMessage(userId, title, content, relatedId));
                log.debug("Sent admin house management message to queue for user {}", userId);
            } else {
                saveMessageDirectly(userId, title, content, "SYSTEM", relatedId);
            }
        } catch (Exception e) {
            log.warn("RabbitMQ unavailable, saving admin house management message directly: {}", e.getMessage());
            saveMessageDirectly(userId, title, content, "SYSTEM", relatedId);
        }
    }

    /**
     * 降级处理：直接调用消息服务将消息保存到数据库（携带关联业务对象 ID）
     *
     * @param userId    接收用户 ID
     * @param title     消息标题
     * @param content   消息内容
     * @param type      消息类型
     * @param relatedId 关联业务对象 ID（可为 null）
     */
    private void saveMessageDirectly(Long userId, String title, String content, String type, Long relatedId) {
        try {
            messageService.sendMessage(userId, title, content, type, relatedId);
        } catch (Exception e) {
            log.error("Failed to save message directly for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * 构建 RabbitMQ 消息体 Map（携带关联业务对象 ID）
     * relatedId 随消息一并传入 MQ，消费者落库时可直接写入 messages.related_id 字段
     *
     * @param userId    接收用户 ID
     * @param title     消息标题
     * @param content   消息内容
     * @param relatedId 关联业务对象 ID（可为 null）
     * @return 包含消息数据的 Map
     */
    private java.util.Map<String, Object> buildMessage(Long userId, String title, String content, Long relatedId) {
        java.util.Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("userId", userId);
        msg.put("title", title);
        msg.put("content", content);
        msg.put("relatedId", relatedId); // 关联业务对象 ID，可为 null
        msg.put("timestamp", System.currentTimeMillis()); // 消息发送时间戳
        return msg;
    }
}
