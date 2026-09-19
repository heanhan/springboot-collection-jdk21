package com.example.ddd.payment.infrastructure.config;

import com.example.ddd.payment.application.port.PaymentPorts;
import com.example.ddd.common.infrastructure.messaging.EventOutbox;
import com.example.ddd.contract.MqTopics;
import com.example.ddd.contract.order.OrderFeignClient;
import org.springframework.context.annotation.*;

/** 基础设施：Feign 和 Outbox 适配应用端口，不吞掉远程错误。 */
@Configuration
public class PaymentConfiguration {
    @Bean PaymentPorts.Orders orders(OrderFeignClient client) { return id -> client.getById(id).requireData(); }
    @Bean PaymentPorts.Events paymentEvents(EventOutbox outbox) {
        return e -> outbox.append(MqTopics.PAYMENT_EVENT+":"+e.getClass().getSimpleName().replaceFirst("Event$",""), e);
    }
}
