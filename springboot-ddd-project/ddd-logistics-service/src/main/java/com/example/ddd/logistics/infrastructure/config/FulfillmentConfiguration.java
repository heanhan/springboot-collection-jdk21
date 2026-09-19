package com.example.ddd.logistics.infrastructure.config;

import com.example.ddd.common.exception.*;
import com.example.ddd.common.infrastructure.messaging.EventOutbox;
import com.example.ddd.contract.MqTopics;
import com.example.ddd.contract.order.*;
import com.example.ddd.contract.payment.PaymentFeignClient;
import com.example.ddd.contract.inventory.*;
import com.example.ddd.logistics.application.port.FulfillmentPorts;
import org.springframework.context.annotation.*;

/**
 * 基础设施装配：显式校验 RPC 业务响应，禁止远程失败时继续发货。
 */
@Configuration
public class FulfillmentConfiguration {
    @Bean
    FulfillmentPorts.Gateway fulfillmentGateway(OrderFeignClient orders, PaymentFeignClient payments, InventoryFeignClient inventory) {
        return new FulfillmentPorts.Gateway() {
            public OrderDTO order(String id) {
                return orders.getById(id).requireData();
            }

            public boolean refunded(String id) {
                return "REFUNDED".equals(payments.getByOrderId(id).requireData().status());
            }

            public void deduct(String id) {
                var result = inventory.deduct(new StockDeductRequest(id)).requireData();
                if (!result.success()) throw new BusinessException(ErrorCode.INVENTORY_LOCK_FAILED);
            }
        };
    }

    @Bean
    FulfillmentPorts.Events shipmentEvents(EventOutbox outbox) {
        return e -> outbox.append(MqTopics.LOGISTICS_EVENT + ":" + e.getClass().getSimpleName().replaceFirst("Event$", ""), e);
    }
}
