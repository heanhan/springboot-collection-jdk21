package com.example.ddd.inventory.infrastructure.mq;

import com.example.ddd.common.domain.event.DomainEvent;
import com.example.ddd.contract.MqTopics;
import com.example.ddd.contract.inventory.event.StockDeductedEvent;
import com.example.ddd.contract.inventory.event.StockLockedEvent;
import com.example.ddd.contract.inventory.event.StockReleasedEvent;
import com.example.ddd.inventory.application.port.DomainEventPublisher;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 领域事件发布器实现。
 *
 * <p><b>Topic 与 Tag：</b>Topic = {@link MqTopics#INVENTORY_EVENT}，
 * Tag 由事件类型决定（JDK 21 pattern matching switch），消费方按 {@code topic + tag} 精确订阅。</p>
 *
 * <p><b>降级策略：</b>RocketMQ 未启动时以 dry-run 模式记录日志，不阻塞库存主流程。
 * 生产环境应替换为"本地事务消息表 + 定时补偿"以保证消息必达。</p>
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
        String destination = MqTopics.INVENTORY_EVENT + ":" + tag;
        if (rocketMQTemplate == null) {
            log.info("[MQ-DRY-RUN] would publish to {} event={}", destination, event);
            return;
        }
        try {
            rocketMQTemplate.syncSend(destination,
                    MessageBuilder.withPayload(event).setHeader("KEYS", event.eventId()).build());
            log.info("[MQ] published topic={} tag={} eventId={}", MqTopics.INVENTORY_EVENT, tag, event.eventId());
        } catch (Exception e) {
            log.error("[MQ] publish failed destination={} event={}", destination, event, e);
        }
    }

    /**
     * 用 JDK 21 pattern matching switch 精确匹配事件类型 -> Tag。
     */
    private String resolveTag(DomainEvent event) {
        return switch (event) {
            case StockLockedEvent e -> MqTopics.TAG_STOCK_LOCKED;
            case StockDeductedEvent e -> MqTopics.TAG_STOCK_DEDUCTED;
            case StockReleasedEvent e -> MqTopics.TAG_STOCK_RELEASED;
            default -> event.getClass().getSimpleName();
        };
    }
}
