package com.houseleasing.mq;

import com.houseleasing.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.rabbitmq.listener.simple.auto-startup", havingValue = "true")
public class MessageConsumer {

    private final MessageService messageService;

    @RabbitListener(queues = "appointment.queue")
    public void handleAppointment(java.util.Map<String, Object> message) {
        try {
            log.info("Received appointment message: {}", message);
            Long userId = message.get("userId") != null ? Long.valueOf(message.get("userId").toString()) : null;
            String title = (String) message.getOrDefault("title", "预约通知");
            String content = (String) message.getOrDefault("content", "");
            if (userId != null) {
                messageService.sendMessage(userId, title, content, "APPOINTMENT");
            }
        } catch (Exception e) {
            log.error("Error handling appointment message: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "contract.queue")
    public void handleContract(java.util.Map<String, Object> message) {
        try {
            log.info("Received contract message: {}", message);
            Long userId = message.get("userId") != null ? Long.valueOf(message.get("userId").toString()) : null;
            String title = (String) message.getOrDefault("title", "合同通知");
            String content = (String) message.getOrDefault("content", "");
            if (userId != null) {
                messageService.sendMessage(userId, title, content, "CONTRACT");
            }
        } catch (Exception e) {
            log.error("Error handling contract message: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "order.queue")
    public void handleOrder(java.util.Map<String, Object> message) {
        try {
            log.info("Received order message: {}", message);
            Long userId = message.get("userId") != null ? Long.valueOf(message.get("userId").toString()) : null;
            String title = (String) message.getOrDefault("title", "订单通知");
            String content = (String) message.getOrDefault("content", "");
            if (userId != null) {
                messageService.sendMessage(userId, title, content, "ORDER");
            }
        } catch (Exception e) {
            log.error("Error handling order message: {}", e.getMessage(), e);
        }
    }
}
