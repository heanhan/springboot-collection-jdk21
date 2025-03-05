package com.example.ai.langchain4j.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: zhaojh
 * @Date: 2025-03-04 14:33
 * @Description:
 */
@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_NAME = "Chat.Queue";
    public static final String EXCHANGE_NAME = "Chat.Exchange";
    public static final String ROUTING_KEY = "Chat.RoutingKey";

    @Bean
    public Queue queue() {
        return new Queue(QUEUE_NAME, true); // true表示持久化
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }
}