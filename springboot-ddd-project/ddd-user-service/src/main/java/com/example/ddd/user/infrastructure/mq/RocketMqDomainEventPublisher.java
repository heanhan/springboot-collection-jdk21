package com.example.ddd.user.infrastructure.mq;

import com.example.ddd.common.domain.event.DomainEvent;
import com.example.ddd.common.util.JsonUtils;
import com.example.ddd.contract.MqTopics;
import com.example.ddd.user.application.port.DomainEventPublisher;
import com.example.ddd.user.domain.model.event.UserDisabledEvent;
import com.example.ddd.user.domain.model.event.UserProfileUpdatedEvent;
import com.example.ddd.user.domain.model.event.UserRegisteredEvent;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 领域事件发布器（DomainEventPublisher 端口的适配器）。
 *
 * <p><b>Topic / Tag 路由策略：</b>
 * 全部发到 {@link MqTopics#USER_EVENT}，Tag = 事件类名。
 * 消费方按 Tag 过滤订阅。</p>
 *
 * <p><b>为什么用 pattern matching switch (JDK 21)？</b>
 * 类型安全 + 编译期穷尽检查（配合 sealed 更佳）。这里 DomainEvent 不是 sealed，
 * 但 switch pattern 依然比 if-else instanceof 优雅。</p>
 *
 * <p><b>如果 RocketMQTemplate 不存在会怎样？</b>
 * 使用 {@code @Autowired(required = false)}，本地未启动 RocketMQ 时不会阻塞应用启动，
 * 事件发布降级为日志输出（学习/开发友好）。</p>
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
        String destination = MqTopics.USER_EVENT + ":" + tag;
        String payload = JsonUtils.toJson(event);

        if (rocketMQTemplate == null) {
            log.warn("[MQ-DRY-RUN] destination={} payload={}", destination, payload);
            return;
        }
        try {
            rocketMQTemplate.syncSend(destination,
                    MessageBuilder.withPayload(payload)
                            .setHeader("KEYS", event.eventId())
                            .build());
            log.info("[MQ] published destination={} eventId={}", destination, event.eventId());
        } catch (Exception e) {
            log.error("[MQ] publish failed destination={} eventId={}", destination, event.eventId(), e);
            throw e;
        }
    }

    private String resolveTag(DomainEvent event) {
        return switch (event) {
            case UserRegisteredEvent e -> MqTopics.TAG_USER_REGISTERED;
            case UserDisabledEvent e -> MqTopics.TAG_USER_DISABLED;
            case UserProfileUpdatedEvent e -> "UserProfileUpdated";
            default -> event.getClass().getSimpleName();
        };
    }
}
