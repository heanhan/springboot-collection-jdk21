package com.example.ddd.order.infrastructure.config;

import com.example.ddd.common.infrastructure.messaging.EventOutbox;
import com.example.ddd.order.application.port.DomainEventPublisher;
import com.example.ddd.order.domain.service.OrderPricingService;
import com.example.ddd.contract.MqTopics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 基础设施装配：纯领域服务和可靠事件发布端口。 */
@Configuration
public class OrderConfiguration {
    @Bean OrderPricingService pricingService() { return new OrderPricingService(); }
    @Bean DomainEventPublisher orderEvents(EventOutbox outbox) {
        return event -> outbox.append(MqTopics.ORDER_EVENT + ":" +
                event.getClass().getSimpleName().replaceFirst("Event$", ""), event);
    }
}
