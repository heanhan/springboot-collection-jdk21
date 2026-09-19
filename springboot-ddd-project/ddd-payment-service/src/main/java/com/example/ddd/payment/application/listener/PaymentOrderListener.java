package com.example.ddd.payment.application.listener;

import com.example.ddd.common.infrastructure.messaging.EventInbox;
import com.example.ddd.contract.MqTopics;
import com.example.ddd.contract.order.event.*;
import com.example.ddd.payment.application.service.PayApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/** 应用层：订单创建/取消消费，不预先写完成标记，失败交由 Broker 重试。 */
@Component
@RocketMQMessageListener(topic=MqTopics.ORDER_EVENT,consumerGroup="ddd-payment-orders",selectorExpression="OrderCreated || OrderCancelled")
public class PaymentOrderListener implements RocketMQListener<MessageExt> {
    private final ObjectMapper json; private final EventInbox inbox; private final PayApplicationService service;
    public PaymentOrderListener(ObjectMapper json,EventInbox inbox,PayApplicationService service) { this.json=json; this.inbox=inbox; this.service=service; }
    public void onMessage(MessageExt message) {
        try {
            if (MqTopics.TAG_ORDER_CREATED.equals(message.getTags())) {
                var e=json.readValue(message.getBody(),OrderCreatedEvent.class);
                inbox.consume("payment-create",e.eventId(),() -> service.create(e));
            } else {
                var e=json.readValue(message.getBody(),OrderCancelledEvent.class);
                inbox.consume("payment-close",e.eventId(),() -> service.close(e.aggregateId()));
            }
        } catch(Exception e) { throw new IllegalStateException("订单事件消费失败",e); }
    }
}
