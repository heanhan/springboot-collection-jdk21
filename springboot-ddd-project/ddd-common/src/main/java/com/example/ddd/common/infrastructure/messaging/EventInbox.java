package com.example.ddd.common.infrastructure.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/** 基础设施：Redis 消费租约和 24 小时完成标记；业务事务成功返回之后才能标记完成。 */
@Component
@ConditionalOnProperty(name = "ddd.messaging.enabled", havingValue = "true")
public class EventInbox {
    private final StringRedisTemplate redis;
    private static final DefaultRedisScript<Long> RELEASE = new DefaultRedisScript<>(
            "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end", Long.class);

    public EventInbox(StringRedisTemplate redis) { this.redis = redis; }

    public void consume(String consumer, String eventId, Runnable action) {
        if (eventId == null || eventId.isBlank()) throw new IllegalArgumentException("事件缺少 eventId");
        String key = "ddd:inbox:" + consumer + ":" + eventId;
        if (Boolean.TRUE.equals(redis.hasKey(key + ":done"))) return;
        String token = UUID.randomUUID().toString();
        if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, token, Duration.ofMinutes(2)))) {
            throw new IllegalStateException("事件处理中，请稍后重试");
        }
        try {
            if (Boolean.TRUE.equals(redis.hasKey(key + ":done"))) return;
            action.run();
            redis.opsForValue().set(key + ":done", "1", Duration.ofHours(24));
        } finally {
            redis.execute(RELEASE, List.of(key), token);
        }
    }
}
