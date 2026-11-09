package com.example.ddd.order.domain.model.event;

import com.example.ddd.common.domain.event.AbstractDomainEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 领域事件：订单已创建。
 *
 * <p><b>领域内部语言：</b>只表达"领域中发生了什么"，不携带任何对外契约
 * (ddd-api-contract) 的类型；对外发布前由基础设施层的
 * {@code OrderIntegrationEventTranslator} 翻译为集成事件。</p>
 */
public class OrderCreatedDomainEvent extends AbstractDomainEvent {

    /** 订单项摘要（领域视角，无独立生命周期）。 */
    public record Item(String skuId, String spuName, int quantity, BigDecimal subtotal) {}

    private final String orderNo;
    private final String userId;
    private final BigDecimal payAmount;
    private final List<Item> items;
    private final LocalDateTime expireAt;

    public OrderCreatedDomainEvent(String orderId, String orderNo, String userId,
                                   BigDecimal payAmount, List<Item> items, LocalDateTime expireAt) {
        super(orderId);
        this.orderNo = orderNo;
        this.userId = userId;
        this.payAmount = payAmount;
        this.items = List.copyOf(items);
        this.expireAt = expireAt;
    }

    public String getOrderNo() { return orderNo; }
    public String getUserId() { return userId; }
    public BigDecimal getPayAmount() { return payAmount; }
    public List<Item> getItems() { return items; }
    public LocalDateTime getExpireAt() { return expireAt; }
}
