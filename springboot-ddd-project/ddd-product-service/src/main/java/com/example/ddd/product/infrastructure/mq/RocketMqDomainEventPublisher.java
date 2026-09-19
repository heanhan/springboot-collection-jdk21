package com.example.ddd.product.infrastructure.mq;

import com.example.ddd.common.domain.event.DomainEvent;
import com.example.ddd.contract.MqTopics;
import com.example.ddd.product.application.port.DomainEventPublisher;
import com.example.ddd.product.domain.model.event.ProductOffShelfEvent;
import com.example.ddd.product.domain.model.event.ProductPublishedEvent;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 领域事件发布器实现。
 *
 * <p><b>Topic 与 Tag 决策：</b>
 * Topic = {@link MqTopics#PRODUCT_EVENT}，Tag 由事件类型决定（pattern matching switch）。
 * 消费方通过 {@code topic + tag} 精确订阅，避免"消息路由全靠 if-else"。</p>
 *
 * <p><b>降级策略：</b>
 * 若 RocketMQ 未启动，本类以 dry-run 模式记录日志，不阻塞业务流程。
 * 生产环境应替换为"本地事务消息表 + 定时补偿"，保证消息必达。</p>
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
        if (event == null) return;
        String tag = resolveTag(event);
        String destination = MqTopics.PRODUCT_EVENT + ":" + tag;
        if (rocketMQTemplate == null) {
            log.info("[MQ-DRY-RUN] would publish to {} event={}", destination, event);
            return;
        }
        try {
            rocketMQTemplate.syncSend(destination,
                    MessageBuilder.withPayload(event).setHeader("KEYS", event.eventId()).build());
            log.info("[MQ] published topic={} tag={} eventId={}", MqTopics.PRODUCT_EVENT, tag, event.eventId());
        } catch (Exception e) {
            log.error("[MQ] publish failed destination={} event={}", destination, event, e);
        }
    }

    /**
     * 用 JDK 21 pattern matching switch 精确匹配事件类型 -> Tag。
     */
    private String resolveTag(DomainEvent event) {
        return switch (event) {
            case ProductPublishedEvent e -> MqTopics.TAG_PRODUCT_PUBLISHED;
            case ProductOffShelfEvent e -> MqTopics.TAG_PRODUCT_OFF_SHELF;
            default -> event.getClass().getSimpleName();
        };
    }
}
