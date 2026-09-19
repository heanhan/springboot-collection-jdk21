package com.example.ddd.common.infrastructure.messaging;

import com.example.ddd.common.domain.event.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.client.producer.SendStatus;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 基础设施：与业务同事务写入 Outbox，后台发送已提交事件；发送失败保留记录重试。 */
@Component
@ConditionalOnProperty(name = "ddd.messaging.enabled", havingValue = "true")
public class EventOutbox {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final RocketMQTemplate mq;

    public EventOutbox(JdbcTemplate jdbc, ObjectMapper json, RocketMQTemplate mq) {
        this.jdbc = jdbc;
        this.json = json;
        this.mq = mq;
    }

    public void append(String destination, DomainEvent event) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Outbox 必须与业务写入处于同一事务");
        }
        try {
            jdbc.update("INSERT INTO t_event_outbox(event_id,destination,payload) VALUES (?,?,?)",
                    event.eventId(), destination, json.writeValueAsString(event));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("领域事件序列化失败", e);
        }
    }

    /** 待发送消息的 JDBC 投影。 */
    private record Pending(String id, String destination, String payload) {}

    /** 至少一次投递：发送成功但标记失败可能重复投递，由消费侧业务幂等兜底。 */
    @Scheduled(fixedDelayString = "${ddd.messaging.poll-ms:1000}")
    public void dispatch() {
        var rows = jdbc.query("SELECT event_id,destination,payload FROM t_event_outbox WHERE sent=0 ORDER BY created_at,event_id LIMIT 100",
                (rs, index) -> new Pending(rs.getString("event_id"), rs.getString("destination"), rs.getString("payload")));
        for (var row : rows) {
            try {
                var result = mq.syncSend(row.destination(), row.payload());
                if (result == null || result.getSendStatus() != SendStatus.SEND_OK) {
                    throw new IllegalStateException("Broker 未确认可靠接收事件");
                }
                jdbc.update("UPDATE t_event_outbox SET sent=1 WHERE event_id=?", row.id());
            } catch (Exception e) {
                LoggerFactory.getLogger(getClass()).warn("事件发送失败，保留待重试 eventId={}", row.id(), e);
            }
        }
    }
}
