package com.example.ddd.order.application.listener;

import com.example.ddd.common.infrastructure.messaging.EventInbox;
import com.example.ddd.contract.MqTopics;
import com.example.ddd.contract.payment.event.PaymentSuccessEvent;
import com.example.ddd.contract.payment.event.RefundSuccessEvent;
import com.example.ddd.contract.logistics.event.*;
import com.example.ddd.contract.order.event.OrderCancelledEvent;
import com.example.ddd.order.application.service.OrderApplicationService;
import com.example.ddd.order.application.port.InventoryGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 应用层事件适配器：只接收已提交事实，事务与状态转换交给应用服务。
 */
public final class OrderEventListeners {
    private OrderEventListeners() {
    }

    /**
     * 支付事实转换成订单事实。
     */
    @Component
    @RocketMQMessageListener(topic = MqTopics.PAYMENT_EVENT, consumerGroup = "ddd-order-payment", selectorExpression = "PaymentSuccess || RefundSuccess")
    public static class Payment implements RocketMQListener<org.apache.rocketmq.common.message.MessageExt> {
        private final ObjectMapper json;
        private final EventInbox inbox;
        private final OrderApplicationService service;
        private final InventoryGateway inventory;

        public Payment(ObjectMapper json, EventInbox inbox, OrderApplicationService service, InventoryGateway inventory) {
            this.json = json;
            this.inbox = inbox;
            this.service = service;
            this.inventory = inventory;
        }

        public void onMessage(org.apache.rocketmq.common.message.MessageExt message) {
            try {
                if (MqTopics.TAG_REFUND_SUCCESS.equals(message.getTags())) {
                    var event = json.readValue(message.getBody(), RefundSuccessEvent.class);
                    inbox.consume("order-refund", event.eventId(), () -> {
                        service.refunded(event.getOrderId());
                        inventory.release(event.getOrderId(), "REFUND");
                    });
                } else {
                    var event = json.readValue(message.getBody(), PaymentSuccessEvent.class);
                    inbox.consume("order-paid", event.eventId(), () -> {
                        if (service.getOrder(event.getOrderId()).getPayAmount().amount().compareTo(event.getAmount()) != 0)
                            throw new IllegalArgumentException("支付金额与订单不符");
                        service.markPaid(event.getOrderId(), event.getChannel(), event.getTradeNo(), event.getPaidAt());
                    });
                }
            } catch (Exception e) {
                throw new IllegalStateException("支付事件消费失败", e);
            }
        }
    }

    /**
     * 发货和签收事件允许乱序抵达；前置状态未到时抛异常交给 MQ 重试。
     */
    @Component
    @RocketMQMessageListener(topic = MqTopics.LOGISTICS_EVENT, consumerGroup = "ddd-order-logistics", selectorExpression = "ShipmentCreated || ShipmentDelivered")
    public static class Logistics implements RocketMQListener<org.apache.rocketmq.common.message.MessageExt> {
        private final ObjectMapper json;
        private final EventInbox inbox;
        private final OrderApplicationService service;

        public Logistics(ObjectMapper json, EventInbox inbox, OrderApplicationService service) {
            this.json = json;
            this.inbox = inbox;
            this.service = service;
        }

        public void onMessage(org.apache.rocketmq.common.message.MessageExt message) {
            try {
                if (MqTopics.TAG_SHIPMENT_CREATED.equals(message.getTags())) {
                    var event = json.readValue(message.getBody(), ShipmentCreatedEvent.class);
                    inbox.consume("order-shipped", event.eventId(), () -> service.ship(event.getOrderId(), event.aggregateId(), event.getCarrier(), event.getTrackingNo()));
                } else {
                    var event = json.readValue(message.getBody(), ShipmentDeliveredEvent.class);
                    inbox.consume("order-completed", event.eventId(), () -> service.complete(event.getOrderId(), event.getUserId()));
                }
            } catch (Exception e) {
                throw new IllegalStateException("物流事件消费失败", e);
            }
        }
    }

    /**
     * 取消事件提交后才释放库存；失败不确认消息，避免永久漏释放。
     */
    @Component
    @RocketMQMessageListener(topic = MqTopics.ORDER_EVENT, consumerGroup = "ddd-order-stock-release", selectorExpression = MqTopics.TAG_ORDER_CANCELLED)
    public static class Cancelled implements RocketMQListener<OrderCancelledEvent> {
        private final EventInbox inbox;
        private final InventoryGateway inventory;

        public Cancelled(EventInbox inbox, InventoryGateway inventory) {
            this.inbox = inbox;
            this.inventory = inventory;
        }

        public void onMessage(OrderCancelledEvent event) {
            inbox.consume("stock-release", event.eventId(), () -> inventory.release(event.aggregateId(), "ORDER_CANCELLED"));
        }
    }
}
