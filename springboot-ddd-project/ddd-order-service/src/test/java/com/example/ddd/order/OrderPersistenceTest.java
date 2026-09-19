package com.example.ddd.order;

import com.example.ddd.common.domain.valueobject.Address;
import com.example.ddd.common.domain.valueobject.Money;
import com.example.ddd.common.infrastructure.messaging.EventOutbox;
import com.example.ddd.order.application.command.PlaceOrderCommand;
import com.example.ddd.order.application.port.*;
import com.example.ddd.order.application.service.OrderApplicationService;
import com.example.ddd.order.domain.model.valueobject.OrderStatus;
import com.example.ddd.order.domain.service.OrderPricingService;
import com.example.ddd.order.infrastructure.persistence.po.OrderPO;
import com.example.ddd.order.infrastructure.persistence.repository.OrderRepositoryImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 订单应用事务：真实迁移/映射/状态重建，远程预占失败补偿及取消事件原子性。 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:order_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=validate", "ddd.messaging.enabled=true"}, showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = OrderPersistenceTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OrderPersistenceTest {
    @Configuration(proxyBeanMethods = false)
    @AutoConfigurationPackage
    @EntityScan(basePackageClasses = OrderPO.class)
    @Import({OrderRepositoryImpl.class, OrderApplicationService.class, EventOutbox.class})
    static class Config {
        @Bean ObjectMapper json() { return new ObjectMapper().findAndRegisterModules(); }
        @Bean OrderPricingService pricing() { return new OrderPricingService(); }
        @Bean DomainEventPublisher events(EventOutbox outbox) {
            return event -> outbox.append("ddd-order-event:" + event.getClass().getSimpleName().replaceFirst("Event$", ""), event);
        }
    }
    @Autowired OrderApplicationService service;
    @Autowired JdbcTemplate jdbc;
    @MockBean ProductGateway products;
    @MockBean InventoryGateway inventory;
    @MockBean RocketMQTemplate mq;
    @SpyBean EventOutbox outbox;

    @BeforeEach void catalog() {
        when(products.getSku("sku")).thenReturn(Optional.of(new ProductGateway.SkuSnapshot(
                "sku", "spu", "商品", "规格", null, Money.ofCny("20.00"), true)));
    }

    @Test void persistCancelAndIgnoreLatePayment() {
        String id = service.placeOrder(command());
        var order = service.getOrder(id);
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getPayAmount().amount()).isEqualByComparingTo("30.00");
        assertThat(eventCount(id)).isEqualTo(1);
        service.cancel(id, "user", "USER", "取消");
        service.cancel(id, "user", "USER", "重复取消");
        service.markPaid(id, "MOCK", "trade", LocalDateTime.now());
        assertThat(service.getOrder(id).getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(eventCount(id)).isEqualTo(2);
        verify(inventory, never()).release(anyString(), anyString());
    }

    @Test void failedOutboxRollsBackOrderAndReleasesReservation() {
        int before = jdbc.queryForObject("SELECT COUNT(*) FROM t_order", Integer.class);
        doThrow(new IllegalStateException("模拟 Outbox 失败")).when(outbox).append(anyString(), any());
        assertThatThrownBy(() -> service.placeOrder(command())).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_order", Integer.class)).isEqualTo(before);
        var orderId = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(inventory).lock(orderId.capture(), anyList());
        verify(inventory).release(orderId.getValue(), "ORDER_ROLLBACK");
        assertThat(eventCount(orderId.getValue())).isZero();
    }

    @Test void scanOnlyUnpaidExpiredOrders() {
        String id = service.placeOrder(command());
        jdbc.update("UPDATE t_order SET expire_at=? WHERE order_id=?", LocalDateTime.now().minusMinutes(1), id);
        assertThat(service.findTimeoutOrders(100)).extracting(o -> o.getOrderId()).contains(id);
        service.cancel(id, null, "TIMEOUT", "超时");
        assertThat(service.findTimeoutOrders(100)).extracting(o -> o.getOrderId()).doesNotContain(id);
    }

    private int eventCount(String id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM t_event_outbox WHERE payload LIKE ?", Integer.class, "%" + id + "%");
    }
    private PlaceOrderCommand command() {
        return new PlaceOrderCommand("user", new Address("北京市", "北京市", "朝阳区", "示例路", "100000", "收件人", "13800000000"),
                "测试", List.of(new PlaceOrderCommand.Item("sku", 1)));
    }
}
