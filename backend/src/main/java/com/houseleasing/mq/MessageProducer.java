package com.houseleasing.mq;

import com.houseleasing.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MessageProducer {

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private MessageService messageService;

    public void sendAppointmentConfirmation(Long userId, String houseTitle) {
        String content = "您的预约申请已提交，房源：" + houseTitle + "，请等待房东确认。";
        try {
            if (rabbitTemplate != null) {
                rabbitTemplate.convertAndSend("house.exchange", "appointment.confirm",
                        buildMessage(userId, "预约确认", content));
                log.debug("Sent appointment confirmation to queue for user {}", userId);
            } else {
                saveMessageDirectly(userId, "预约确认", content, "APPOINTMENT");
            }
        } catch (Exception e) {
            log.warn("RabbitMQ unavailable, saving message directly: {}", e.getMessage());
            saveMessageDirectly(userId, "预约确认", content, "APPOINTMENT");
        }
    }

    public void sendContractStatusChange(Long userId, String contractStatus) {
        String content = "合同状态更新：" + contractStatus;
        try {
            if (rabbitTemplate != null) {
                rabbitTemplate.convertAndSend("house.exchange", "contract.status",
                        buildMessage(userId, "合同状态通知", content));
                log.debug("Sent contract status change to queue for user {}", userId);
            } else {
                saveMessageDirectly(userId, "合同状态通知", content, "CONTRACT");
            }
        } catch (Exception e) {
            log.warn("RabbitMQ unavailable, saving message directly: {}", e.getMessage());
            saveMessageDirectly(userId, "合同状态通知", content, "CONTRACT");
        }
    }

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

    private void saveMessageDirectly(Long userId, String title, String content, String type) {
        try {
            messageService.sendMessage(userId, title, content, type);
        } catch (Exception e) {
            log.error("Failed to save message directly for user {}: {}", userId, e.getMessage());
        }
    }

    private java.util.Map<String, Object> buildMessage(Long userId, String title, String content) {
        java.util.Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("userId", userId);
        msg.put("title", title);
        msg.put("content", content);
        msg.put("timestamp", System.currentTimeMillis());
        return msg;
    }
}
