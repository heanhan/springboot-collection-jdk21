package com.example.ddd.auth.infrastructure.mq;

import com.example.ddd.auth.application.port.DomainEventPublisher;
import com.example.ddd.common.domain.event.DomainEvent;
import com.example.ddd.common.util.JsonUtils;
import com.example.ddd.contract.MqTopics;
import com.example.ddd.contract.auth.event.AccountCreatedEvent;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 事件发布器（auth-service 侧）。
 */
@Component
public class RocketMqDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RocketMqDomainEventPublisher.class);

    private final RocketMQTemplate rocketMQTemplate;

    public RocketMqDomainEventPublisher(@Autowired(required = false) RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    @Override
    public void publish(DomainEvent event) {
        String tag = resolveTag(event);
        String destination = MqTopics.AUTH_EVENT + ":" + tag;
        String payload = JsonUtils.toJson(event);

        if (rocketMQTemplate == null) {
            log.warn("[MQ-DRY-RUN] destination={} payload={}", destination, payload);
            return;
        }
        try {
            rocketMQTemplate.syncSend(destination,
                    MessageBuilder.withPayload(payload).setHeader("KEYS", event.eventId()).build());
            log.info("[MQ] published destination={} eventId={}", destination, event.eventId());
        } catch (Exception e) {
            log.error("[MQ] publish failed destination={}", destination, e);
            throw e;
        }
    }

    private String resolveTag(DomainEvent event) {
        return switch (event) {
            case AccountCreatedEvent e -> MqTopics.TAG_ACCOUNT_CREATED;
            default -> event.getClass().getSimpleName();
        };
    }
}
