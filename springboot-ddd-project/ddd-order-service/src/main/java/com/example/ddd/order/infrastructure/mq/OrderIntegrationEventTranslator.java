package com.example.ddd.order.infrastructure.mq;

import com.example.ddd.common.domain.event.DomainEvent;
import com.example.ddd.contract.order.event.OrderCancelledEvent;
import com.example.ddd.contract.order.event.OrderCompletedEvent;
import com.example.ddd.contract.order.event.OrderCreatedEvent;
import com.example.ddd.contract.order.event.OrderPaidEvent;
import com.example.ddd.contract.order.event.OrderShippedEvent;
import com.example.ddd.order.domain.model.event.OrderCancelledDomainEvent;
import com.example.ddd.order.domain.model.event.OrderCompletedDomainEvent;
import com.example.ddd.order.domain.model.event.OrderCreatedDomainEvent;
import com.example.ddd.order.domain.model.event.OrderPaidDomainEvent;
import com.example.ddd.order.domain.model.event.OrderShippedDomainEvent;

/**
 * 基础设施：领域事件 → 集成事件（发布语言）翻译器。
 *
 * <p><b>为什么需要翻译？</b>领域层收集的是"领域内部语言"（{@code *DomainEvent}），
 * 而跨上下文协作要求对外契约（ddd-api-contract）的集成事件——两者的稳定性不同：
 * 领域事件随领域模型演进，集成事件是承诺兼容的发布语言。翻译发生在出站边界，
 * 从而保证 domain 不依赖 contract（由 ArchUnit 规则守护）。</p>
 *
 * <p><b>MQ Tag 说明：</b>发布方按翻译后的集成事件类名推导 Tag
 * （{@code SimpleName - "Event"}），与改造前完全一致，下游消费者无需变更。</p>
 */
public final class OrderIntegrationEventTranslator {

    private OrderIntegrationEventTranslator() {}

    /**
     * 将订单领域事件翻译为同数据的 contract 集成事件。
     *
     * @throws IllegalStateException 未登记的领域事件类型（新增事件时必须补充映射）
     */
    public static DomainEvent translate(DomainEvent event) {
        return switch (event) {
            case OrderCreatedDomainEvent e -> new OrderCreatedEvent(e.aggregateId(), e.getOrderNo(), e.getUserId(),
                    e.getPayAmount(),
                    e.getItems().stream()
                            .map(i -> new OrderCreatedEvent.Item(i.skuId(), i.spuName(), i.quantity(), i.subtotal()))
                            .toList(),
                    e.getExpireAt());
            case OrderPaidDomainEvent e -> new OrderPaidEvent(e.aggregateId(), e.getOrderNo(), e.getUserId(),
                    e.getPayAmount(), e.getChannel(), e.getTradeNo(), e.getPaidAt());
            case OrderShippedDomainEvent e -> new OrderShippedEvent(e.aggregateId(), e.getOrderNo(), e.getUserId(),
                    e.getShipmentId(), e.getCarrier(), e.getTrackingNo(), e.getShippedAt());
            case OrderCompletedDomainEvent e -> new OrderCompletedEvent(e.aggregateId(), e.getOrderNo(), e.getUserId(),
                    e.getCompletedAt());
            case OrderCancelledDomainEvent e -> new OrderCancelledEvent(e.aggregateId(), e.getOrderNo(), e.getUserId(),
                    e.getReasonCode(), e.getReasonDesc());
            default -> throw new IllegalStateException("未登记的订单领域事件: " + event.getClass().getName());
        };
    }
}
