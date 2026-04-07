package com.houseleasing.mq;

import com.houseleasing.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * RabbitMQ 消息消费者
 *
 * @author hongwenhao
 * @description 监听 RabbitMQ 各类消息队列，消费消息后将通知写入数据库，
 *              仅在 spring.rabbitmq.listener.simple.auto-startup=true 时启用
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.rabbitmq.listener.simple.auto-startup", havingValue = "true")
public class MessageConsumer {

    private final MessageService messageService;

    /**
     * 处理预约确认消息队列中的消息
     * 解析消息内容并写入数据库通知，relatedId 对应关联的订单 ID
     *
     * @param message 从队列接收的消息（Map 格式，包含 userId、title、content、relatedId）
     */
    @RabbitListener(queues = "appointment.queue")
    public void handleAppointment(java.util.Map<String, Object> message) {
        try {
            log.info("Received appointment message: {}", message);
            Long userId = message.get("userId") != null ? Long.valueOf(message.get("userId").toString()) : null;
            String title = (String) message.getOrDefault("title", "预约通知");
            String content = (String) message.getOrDefault("content", "");
            // 解析关联的订单 ID，允许为 null
            Long relatedId = message.get("relatedId") != null ? Long.valueOf(message.get("relatedId").toString()) : null;
            if (userId != null) {
                // 将消费的消息通过消息服务持久化到数据库，同时记录关联业务对象 ID
                messageService.sendMessage(userId, title, content, "APPOINTMENT", relatedId);
            }
        } catch (Exception e) {
            log.error("Error handling appointment message: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理合同状态通知消息队列中的消息
     * relatedId 对应关联的合同 ID
     *
     * @param message 从队列接收的消息（Map 格式）
     */
    @RabbitListener(queues = "contract.queue")
    public void handleContract(java.util.Map<String, Object> message) {
        try {
            log.info("Received contract message: {}", message);
            Long userId = message.get("userId") != null ? Long.valueOf(message.get("userId").toString()) : null;
            String title = (String) message.getOrDefault("title", "合同通知");
            String content = (String) message.getOrDefault("content", "");
            Long relatedId = message.get("relatedId") != null ? Long.valueOf(message.get("relatedId").toString()) : null;
            if (userId != null) {
                messageService.sendMessage(userId, title, content, "CONTRACT", relatedId);
            }
        } catch (Exception e) {
            log.error("Error handling contract message: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理订单状态通知消息队列中的消息
     * relatedId 对应关联的订单 ID
     *
     * @param message 从队列接收的消息（Map 格式）
     */
    @RabbitListener(queues = "order.queue")
    public void handleOrder(java.util.Map<String, Object> message) {
        try {
            log.info("Received order message: {}", message);
            Long userId = message.get("userId") != null ? Long.valueOf(message.get("userId").toString()) : null;
            String title = (String) message.getOrDefault("title", "订单通知");
            String content = (String) message.getOrDefault("content", "");
            Long relatedId = message.get("relatedId") != null ? Long.valueOf(message.get("relatedId").toString()) : null;
            if (userId != null) {
                messageService.sendMessage(userId, title, content, "ORDER", relatedId);
            }
        } catch (Exception e) {
            log.error("Error handling order message: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理登录提醒消息队列中的消息
     * 登录事件无关联业务对象，relatedId 始终为 null
     *
     * @param message 从队列接收的消息（Map 格式）
     */
    @RabbitListener(queues = "login.queue")
    public void handleLogin(java.util.Map<String, Object> message) {
        try {
            log.info("Received login message: {}", message);
            Long userId = message.get("userId") != null ? Long.valueOf(message.get("userId").toString()) : null;
            String title = (String) message.getOrDefault("title", "登录提醒");
            String content = (String) message.getOrDefault("content", "");
            if (userId != null) {
                // 登录通知无关联业务对象，relatedId 传 null
                messageService.sendMessage(userId, title, content, "SYSTEM", null);
            }
        } catch (Exception e) {
            log.error("Error handling login message: {}", e.getMessage(), e);
        }
    }
}
