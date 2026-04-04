package com.houseleasing.mq;

import com.houseleasing.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 消息生产者
 *
 * @author HouseLeasingSystem开发团队
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
     */
    public void sendAppointmentConfirmation(Long userId, String houseTitle) {
        String content = "您的预约申请已提交，房源：" + houseTitle + "，请等待房东确认。";
        try {
            if (rabbitTemplate != null) {
                // RabbitMQ 可用时，发送消息到队列异步处理
                rabbitTemplate.convertAndSend("house.exchange", "appointment.confirm",
                        buildMessage(userId, "预约确认", content));
                log.debug("Sent appointment confirmation to queue for user {}", userId);
            } else {
                // RabbitMQ 不可用，直接保存消息到数据库
                saveMessageDirectly(userId, "预约确认", content, "APPOINTMENT");
            }
        } catch (Exception e) {
            log.warn("RabbitMQ unavailable, saving message directly: {}", e.getMessage());
            saveMessageDirectly(userId, "预约确认", content, "APPOINTMENT");
        }
    }

    /**
     * 发送合同状态变更通知消息
     * 通知用户合同的最新状态（如已签署、已取消等）
     *
     * @param userId         接收通知的用户 ID
     * @param contractStatus 合同状态描述
     */
    public void sendContractStatusChange(Long userId, String contractStatus) {
        String content = "合同状态更新：" + contractStatus;
        try {
            if (rabbitTemplate != null) {
                // RabbitMQ可用：发送到队列异步处理
                rabbitTemplate.convertAndSend("house.exchange", "contract.status",
                        buildMessage(userId, "合同状态通知", content));
                log.debug("已把该用户的合约状态变更请求放入消息队列，等待处理 {}", userId);
            } else {
                saveMessageDirectly(userId, "合同状态通知", content, "CONTRACT");
            }
        } catch (Exception e) {
            log.warn("由于RabbitMQ 服务不可用，系统已将消息暂存至本地（或数据库），等待后续处理: {}", e.getMessage());
            saveMessageDirectly(userId, "合同状态通知", content, "CONTRACT");
        }
    }

    /**
     * 发送订单状态变更通知消息
     * 通知用户订单的最新状态（如已批准、已拒绝等）
     *
     * @param userId      接收通知的用户 ID
     * @param orderStatus 订单状态描述
     */
    public void sendOrderStatusChange(Long userId, String orderStatus) {
        String content = "订单状态更新：" + orderStatus;
        try {
            if (rabbitTemplate != null) {
                rabbitTemplate.convertAndSend("house.exchange", "order.status",
                        buildMessage(userId, "订单状态通知", content));
                log.debug("Sent order status change to queue for user {}", userId);
            } else {
                saveMessageDirectly(userId, "订单状态通知", content, "ORDER");
            }
        } catch (Exception e) {
            log.warn("RabbitMQ unavailable, saving message directly: {}", e.getMessage());
            saveMessageDirectly(userId, "订单状态通知", content, "ORDER");
        }
    }

    /**
     * 发送登录提醒消息
     * 登录属于关键操作，需要在消息中心保留可追溯记录。
     *
     * @param userId  接收通知的用户 ID
     * @param content 登录提醒正文
     */
    public void sendLoginNotification(Long userId, String content) {
        try {
            if (rabbitTemplate != null) {
                // 登录提醒使用独立路由键 login.*，便于按消息类型解耦与监控。
                rabbitTemplate.convertAndSend("house.exchange", "login.notice",
                        buildMessage(userId, "登录提醒", content));
                log.debug("Sent login notification to queue for user {}", userId);
            } else {
                saveMessageDirectly(userId, "登录提醒", content, "SYSTEM");
            }
        } catch (Exception e) {
            log.warn("RabbitMQ unavailable, saving login message directly: {}", e.getMessage());
            saveMessageDirectly(userId, "登录提醒", content, "SYSTEM");
        }
    }

    /**
     * 降级处理：直接调用消息服务将消息保存到数据库
     *
     * @param userId  接收用户 ID
     * @param title   消息标题
     * @param content 消息内容
     * @param type    消息类型
     */
    private void saveMessageDirectly(Long userId, String title, String content, String type) {
        try {
            messageService.sendMessage(userId, title, content, type);
        } catch (Exception e) {
            log.error("Failed to save message directly for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * 构建 RabbitMQ 消息体 Map
     *
     * @param userId  接收用户 ID
     * @param title   消息标题
     * @param content 消息内容
     * @return 包含消息数据的 Map
     */
    private java.util.Map<String, Object> buildMessage(Long userId, String title, String content) {
        java.util.Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("userId", userId);
        msg.put("title", title);
        msg.put("content", content);
        msg.put("timestamp", System.currentTimeMillis()); // 消息发送时间戳
        return msg;
    }
}
