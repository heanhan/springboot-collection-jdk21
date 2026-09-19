package com.example.ddd.logistics.application.listener;

import com.example.ddd.common.infrastructure.messaging.EventInbox;
import com.example.ddd.contract.MqTopics;
import com.example.ddd.contract.order.event.OrderPaidEvent;
import com.example.ddd.logistics.application.service.ShipApplicationService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/** 应用层：订单支付完成才进入履约；消息重复依靠 Redis 和订单唯一键双重幂等。 */
@Component
@RocketMQMessageListener(topic=MqTopics.ORDER_EVENT,consumerGroup="ddd-logistics-paid",selectorExpression=MqTopics.TAG_ORDER_PAID)
public class OrderPaidListener implements RocketMQListener<OrderPaidEvent> {
    private final EventInbox inbox; private final ShipApplicationService service;
    public OrderPaidListener(EventInbox inbox,ShipApplicationService service) { this.inbox=inbox; this.service=service; }
    public void onMessage(OrderPaidEvent event) { inbox.consume("shipment-create",event.eventId(),() -> service.ship(event.aggregateId())); }
}
