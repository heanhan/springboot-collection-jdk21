package com.example.ddd.payment;

import com.example.ddd.common.infrastructure.messaging.EventOutbox;
import com.example.ddd.contract.order.OrderDTO;
import com.example.ddd.payment.application.port.PaymentPorts;
import com.example.ddd.payment.application.service.PayApplicationService;
import com.example.ddd.payment.domain.model.aggregate.PaymentOrder;
import com.example.ddd.payment.domain.repository.PaymentRepository;
import com.example.ddd.payment.infrastructure.channel.MockChannels;
import com.example.ddd.payment.infrastructure.persistence.po.PaymentPO;
import com.example.ddd.payment.infrastructure.persistence.repository.PaymentRepositoryImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** H2 + Flyway + JPA 事务回归，不启动 HTTP、Redis 或 Broker；覆盖并发回调和 Outbox 原子性。 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:payment_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=validate", "ddd.messaging.enabled=true"}, showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = PaymentPersistenceTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PaymentPersistenceTest {
    @Configuration(proxyBeanMethods = false)
    @org.springframework.boot.autoconfigure.AutoConfigurationPackage
    @EntityScan(basePackageClasses = PaymentPO.class)
    @Import({PaymentRepositoryImpl.class, PayApplicationService.class, MockChannels.class, EventOutbox.class})
    static class Config {
        @Bean ObjectMapper json() { return new ObjectMapper().findAndRegisterModules(); }
        @Bean PaymentPorts.Events events(EventOutbox outbox) {
            return event -> outbox.append("ddd-payment-event:" + event.getClass().getSimpleName().replaceFirst("Event$", ""), event);
        }
    }

    @Autowired PaymentRepository repository;
    @Autowired PayApplicationService service;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JdbcTemplate jdbc;
    @MockBean PaymentPorts.Orders orders;
    @MockBean RocketMQTemplate mq;
    @SpyBean EventOutbox outbox;
    String id;

    @BeforeEach void seed() {
        id = UUID.randomUUID().toString();
        new TransactionTemplate(transactions).executeWithoutResult(status -> repository.save(
                PaymentOrder.create(id, id, "no", "user", BigDecimal.TEN, LocalDateTime.now().plusMinutes(30))));
        var order = mock(OrderDTO.class);
        when(order.status()).thenReturn("CREATED");
        when(orders.get(id)).thenReturn(order);
    }

    @Test void concurrentCallbacksAndDuplicateRefundPublishOnce() throws Exception {
        var start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Callable<Void> callback = () -> { start.await(); service.callback(id, "user", BigDecimal.TEN, "trade"); return null; };
            var first = executor.submit(callback);
            var second = executor.submit(callback);
            start.countDown();
            first.get(10, TimeUnit.SECONDS); second.get(10, TimeUnit.SECONDS);
        }
        assertThat(service.get(id).status()).isEqualTo(PaymentOrder.Status.SUCCESS);
        assertThat(eventCount()).isEqualTo(1);
        var refund = service.refund(id, "user", "退款");
        assertThat(service.refund(id, "user", "重复退款").refundId()).isEqualTo(refund.refundId());
        assertThat(service.get(id).status()).isEqualTo(PaymentOrder.Status.REFUNDED);
        assertThat(eventCount()).isEqualTo(2);
    }

    @Test void failedOutboxAppendRollsBackPayment() {
        doThrow(new IllegalStateException("模拟 Outbox 失败")).when(outbox).append(anyString(), any());
        assertThatThrownBy(() -> service.callback(id, "user", BigDecimal.TEN, "trade"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(service.get(id).status()).isEqualTo(PaymentOrder.Status.PENDING);
        assertThat(eventCount()).isZero();
    }

    @Test void cancellationAfterPaymentCompensatesExactlyOnce() {
        service.callback(id, "user", BigDecimal.TEN, "trade");
        service.close(id); service.close(id);
        assertThat(service.get(id).status()).isEqualTo(PaymentOrder.Status.REFUNDED);
        assertThat(eventCount()).isEqualTo(2);
    }

    private int eventCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM t_event_outbox WHERE payload LIKE ?", Integer.class, "%" + id + "%");
    }
}
