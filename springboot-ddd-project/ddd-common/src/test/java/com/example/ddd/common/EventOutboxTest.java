package com.example.ddd.common;

import com.example.ddd.common.domain.event.AbstractDomainEvent;
import com.example.ddd.common.infrastructure.messaging.EventOutbox;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** H2 事务测试：回滚不留事件，投递失败保留重试，成功后不再发送。 */
class EventOutboxTest {
    /** 测试事件，不依赖业务上下文。 */
    static class SampleEvent extends AbstractDomainEvent { SampleEvent() { super("aggregate"); } }
    @Test void rollbackAndRetry() {
        var ds=new JdbcDataSource(); ds.setURL("jdbc:h2:mem:outbox;DB_CLOSE_DELAY=-1");
        var jdbc=new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE t_event_outbox(event_id VARCHAR PRIMARY KEY,destination VARCHAR,payload CLOB,sent INT DEFAULT 0,created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        var mq=mock(RocketMQTemplate.class);
        var outbox=new EventOutbox(jdbc,new ObjectMapper().findAndRegisterModules(),mq);
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        assertThatThrownBy(() -> outbox.append("topic:tag",new SampleEvent())).isInstanceOf(IllegalStateException.class);
        tx.executeWithoutResult(s -> { outbox.append("topic:tag",new SampleEvent()); s.setRollbackOnly(); });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_event_outbox",Integer.class)).isZero();
        tx.executeWithoutResult(s -> outbox.append("topic:tag",new SampleEvent()));
        var timeout=mock(SendResult.class); when(timeout.getSendStatus()).thenReturn(SendStatus.FLUSH_DISK_TIMEOUT);
        var success=mock(SendResult.class); when(success.getSendStatus()).thenReturn(SendStatus.SEND_OK);
        when(mq.syncSend(anyString(),any(Object.class)))
                .thenThrow(new IllegalStateException("模拟发送故障")).thenReturn(timeout).thenReturn(null).thenReturn(success);
        outbox.dispatch();
        assertThat(jdbc.queryForObject("SELECT sent FROM t_event_outbox",Integer.class)).isZero();
        outbox.dispatch();
        assertThat(jdbc.queryForObject("SELECT sent FROM t_event_outbox",Integer.class)).isZero();
        outbox.dispatch();
        assertThat(jdbc.queryForObject("SELECT sent FROM t_event_outbox",Integer.class)).isZero();
        outbox.dispatch(); outbox.dispatch();
        assertThat(jdbc.queryForObject("SELECT sent FROM t_event_outbox",Integer.class)).isEqualTo(1);
        verify(mq,times(4)).syncSend(anyString(),any(Object.class));
    }
}
