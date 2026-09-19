package com.example.ddd.common;

import com.example.ddd.common.infrastructure.messaging.EventInbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.time.Duration;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 消费幂等回归：失败不能写完成标记，竞争中的消息必须重试，完成后才可跳过。 */
class EventInboxTest {
    StringRedisTemplate redis;
    ValueOperations<String, String> values;
    EventInbox inbox;
    String key = "ddd:inbox:consumer:event";

    @BeforeEach @SuppressWarnings("unchecked") void setup() {
        redis = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(eq(key), anyString(), eq(Duration.ofMinutes(2)))).thenReturn(true);
        inbox = new EventInbox(redis);
    }
    @Test void failedWorkIsRetriedBeforeMarkingDone() {
        Runnable action = mock(Runnable.class);
        doThrow(new IllegalStateException("模拟失败")).doNothing().when(action).run();
        assertThatThrownBy(() -> inbox.consume("consumer", "event", action)).isInstanceOf(IllegalStateException.class);
        verify(values, never()).set(eq(key + ":done"), anyString(), any(Duration.class));
        inbox.consume("consumer", "event", action);
        verify(action, times(2)).run();
        verify(values).set(key + ":done", "1", Duration.ofHours(24));
    }
    @Test void leasedEventMustNotBeAcknowledged() {
        when(values.setIfAbsent(eq(key), anyString(), eq(Duration.ofMinutes(2)))).thenReturn(false);
        Runnable action = mock(Runnable.class);
        assertThatThrownBy(() -> inbox.consume("consumer", "event", action)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(action);
    }
    @Test void completedEventIsSkipped() {
        when(redis.hasKey(key + ":done")).thenReturn(true);
        Runnable action = mock(Runnable.class);
        inbox.consume("consumer", "event", action);
        verifyNoInteractions(action);
        verify(values, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
    }
}
