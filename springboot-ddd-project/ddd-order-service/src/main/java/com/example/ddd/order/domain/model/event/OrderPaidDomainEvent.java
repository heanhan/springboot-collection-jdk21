package com.example.ddd.order.domain.model.event;

import com.example.ddd.common.domain.event.AbstractDomainEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 领域事件：订单已支付（CREATED → PAID）。
 *
 * <p>领域内部语言，对外发布前由基础设施层翻译为集成事件。</p>
 */
public class OrderPaidDomainEvent extends AbstractDomainEvent {

    private final String orderNo;
    private final String userId;
    private final BigDecimal payAmount;
    private final String channel;
    private final String tradeNo;
    private final LocalDateTime paidAt;

    public OrderPaidDomainEvent(String orderId, String orderNo, String userId, BigDecimal payAmount,
                                String channel, String tradeNo, LocalDateTime paidAt) {
        super(orderId);
        this.orderNo = orderNo;
        this.userId = userId;
        this.payAmount = payAmount;
        this.channel = channel;
        this.tradeNo = tradeNo;
        this.paidAt = paidAt;
    }

    public String getOrderNo() { return orderNo; }
    public String getUserId() { return userId; }
    public BigDecimal getPayAmount() { return payAmount; }
    public String getChannel() { return channel; }
    public String getTradeNo() { return tradeNo; }
    public LocalDateTime getPaidAt() { return paidAt; }
}
