package com.example.ddd.order.infrastructure.config;

import com.example.ddd.common.domain.event.DomainEvent;
import com.example.ddd.common.infrastructure.messaging.EventOutbox;
import com.example.ddd.order.application.port.DomainEventPublisher;
import com.example.ddd.order.domain.service.OrderPricingService;
import com.example.ddd.order.infrastructure.mq.OrderIntegrationEventTranslator;
import com.example.ddd.contract.MqTopics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 基础设施装配：纯领域服务和可靠事件发布端口。 */
@Configuration
public class OrderConfiguration {
    @Bean OrderPricingService pricingService() { return new OrderPricingService(); }
    @Bean DomainEventPublisher orderEvents(EventOutbox outbox) {
        // 出站边界：先把领域事件翻译为 contract 集成事件，Tag / payload 与改造前保持一致
        return event -> {
            DomainEvent integration = OrderIntegrationEventTranslator.translate(event);
            outbox.append(MqTopics.ORDER_EVENT + ":" +
                    integration.getClass().getSimpleName().replaceFirst("Event$", ""), integration);
        };
    }
}
