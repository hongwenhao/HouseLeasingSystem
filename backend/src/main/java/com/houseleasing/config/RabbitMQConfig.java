package com.houseleasing.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RabbitMQConfig {

    public static final String HOUSE_EXCHANGE = "house.exchange";
    public static final String APPOINTMENT_QUEUE = "appointment.queue";
    public static final String CONTRACT_QUEUE = "contract.queue";
    public static final String ORDER_QUEUE = "order.queue";

    @Bean
    public TopicExchange houseExchange() {
        return ExchangeBuilder.topicExchange(HOUSE_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue appointmentQueue() {
        return QueueBuilder.durable(APPOINTMENT_QUEUE).build();
    }

    @Bean
    public Queue contractQueue() {
        return QueueBuilder.durable(CONTRACT_QUEUE).build();
    }

    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(ORDER_QUEUE).build();
    }

    @Bean
    public Binding appointmentBinding(Queue appointmentQueue, TopicExchange houseExchange) {
        return BindingBuilder.bind(appointmentQueue).to(houseExchange).with("appointment.*");
    }

    @Bean
    public Binding contractBinding(Queue contractQueue, TopicExchange houseExchange) {
        return BindingBuilder.bind(contractQueue).to(houseExchange).with("contract.*");
    }

    @Bean
    public Binding orderBinding(Queue orderQueue, TopicExchange houseExchange) {
        return BindingBuilder.bind(orderQueue).to(houseExchange).with("order.*");
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
