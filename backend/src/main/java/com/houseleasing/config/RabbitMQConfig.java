package com.houseleasing.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 消息队列配置类
 *
 * @author hongwenhao
 * @description 配置 RabbitMQ 的交换机、队列和绑定关系，使用 Topic 交换机路由消息，
 *              消息使用 JSON 序列化格式
 */
@Slf4j
@Configuration
public class RabbitMQConfig {

    /** 主题交换机名称（所有消息通过此交换机路由） */
    public static final String HOUSE_EXCHANGE = "house.exchange";
    /** 预约消息队列名称 */
    public static final String APPOINTMENT_QUEUE = "appointment.queue";
    /** 合同消息队列名称 */
    public static final String CONTRACT_QUEUE = "contract.queue";
    /** 订单消息队列名称 */
    public static final String ORDER_QUEUE = "order.queue";
    /** 登录消息队列名称 */
    public static final String LOGIN_QUEUE = "login.queue";

    /**
     * 创建持久化 Topic 交换机
     * Topic 交换机支持通配符路由（* 匹配一个单词，# 匹配多个单词）
     *
     * @return Topic 交换机实例
     */
    @Bean
    public TopicExchange houseExchange() {
        return ExchangeBuilder.topicExchange(HOUSE_EXCHANGE).durable(true).build();
    }

    /**
     * 创建持久化预约消息队列
     *
     * @return 预约消息队列实例
     */
    @Bean
    public Queue appointmentQueue() {
        return QueueBuilder.durable(APPOINTMENT_QUEUE).build();
    }

    /**
     * 创建持久化合同消息队列
     *
     * @return 合同消息队列实例
     */
    @Bean
    public Queue contractQueue() {
        return QueueBuilder.durable(CONTRACT_QUEUE).build();
    }

    /**
     * 创建持久化订单消息队列
     *
     * @return 订单消息队列实例
     */
    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(ORDER_QUEUE).build();
    }

    /**
     * 创建持久化登录消息队列
     *
     * @return 登录消息队列实例
     */
    @Bean
    public Queue loginQueue() {
        return QueueBuilder.durable(LOGIN_QUEUE).build();
    }

    /**
     * 将预约队列绑定到交换机，路由键模式为 appointment.*
     *
     * @param appointmentQueue 预约队列
     * @param houseExchange    交换机
     * @return 绑定关系
     */
    @Bean
    public Binding appointmentBinding(Queue appointmentQueue, TopicExchange houseExchange) {
        return BindingBuilder.bind(appointmentQueue).to(houseExchange).with("appointment.*");
    }

    /**
     * 将合同队列绑定到交换机，路由键模式为 contract.*
     *
     * @param contractQueue 合同队列
     * @param houseExchange 交换机
     * @return 绑定关系
     */
    @Bean
    public Binding contractBinding(Queue contractQueue, TopicExchange houseExchange) {
        return BindingBuilder.bind(contractQueue).to(houseExchange).with("contract.*");
    }

    /**
     * 将订单队列绑定到交换机，路由键模式为 order.*
     *
     * @param orderQueue    订单队列
     * @param houseExchange 交换机
     * @return 绑定关系
     */
    @Bean
    public Binding orderBinding(Queue orderQueue, TopicExchange houseExchange) {
        return BindingBuilder.bind(orderQueue).to(houseExchange).with("order.*");
    }

    /**
     * 将登录队列绑定到交换机，路由键模式为 login.*
     *
     * @param loginQueue    登录队列
     * @param houseExchange 交换机
     * @return 绑定关系
     */
    @Bean
    public Binding loginBinding(Queue loginQueue, TopicExchange houseExchange) {
        return BindingBuilder.bind(loginQueue).to(houseExchange).with("login.*");
    }

    /**
     * 配置消息转换器，使用 Jackson JSON 格式序列化消息
     *
     * @return Jackson JSON 消息转换器
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 配置 RabbitTemplate，注入 JSON 消息转换器
     *
     * @param connectionFactory RabbitMQ 连接工厂
     * @return 配置好的 RabbitTemplate 实例
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter()); // 设置 JSON 消息格式
        return template;
    }
}
