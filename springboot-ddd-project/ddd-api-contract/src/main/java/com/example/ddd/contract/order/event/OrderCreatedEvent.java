package com.example.ddd.contract.order.event;

import com.example.ddd.common.domain.event.AbstractDomainEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 领域事件：订单已创建 (OrderCreated)。
 *
 * <p><b>发布者：</b>order-service，在下单事务提交后。</p>
 * <p><b>订阅者：</b>payment-service（创建待支付的支付单）。</p>
 *
 * <p><b>为什么用具体类而不是通用 Map？</b>
 * 事件的字段就是发布语言的一部分，强类型有助于消费方在编译期发现问题，
 * 也便于用 IDE 追溯"谁在关心这个事件"。</p>
 *
 * @author ddd-learning
 */
public class OrderCreatedEvent extends AbstractDomainEvent {

    private static final long serialVersionUID = 1L;

    /** 订单号（对用户展示） */
    private String orderNo;

    /** 下单用户 ID */
    private String userId;

    /** 应付金额（用于支付单创建） */
    private BigDecimal payAmount;

    /** 订单项摘要（供后续展示 / 审计） */
    private List<Item> items;

    /** 订单过期时间（超过则自动关单） */
    private LocalDateTime expireAt;

    public OrderCreatedEvent() {
        super();
    }

    public OrderCreatedEvent(String orderId, String orderNo, String userId,
                             BigDecimal payAmount, List<Item> items, LocalDateTime expireAt) {
        super(orderId);
        this.orderNo = orderNo;
        this.userId = userId;
        this.payAmount = payAmount;
        this.items = items;
        this.expireAt = expireAt;
    }

    public String getOrderNo() { return orderNo; }
    public String getUserId() { return userId; }
    public BigDecimal getPayAmount() { return payAmount; }
    public List<Item> getItems() { return items; }
    public LocalDateTime getExpireAt() { return expireAt; }

    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setPayAmount(BigDecimal payAmount) { this.payAmount = payAmount; }
    public void setItems(List<Item> items) { this.items = items; }
    public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }

    /**
     * 订单项摘要。
     */
    public static class Item {
        private String skuId;
        private String spuName;
        private int quantity;
        private BigDecimal subtotal;

        public Item() {}

        public Item(String skuId, String spuName, int quantity, BigDecimal subtotal) {
            this.skuId = skuId;
            this.spuName = spuName;
            this.quantity = quantity;
            this.subtotal = subtotal;
        }

        public String getSkuId() { return skuId; }
        public String getSpuName() { return spuName; }
        public int getQuantity() { return quantity; }
        public BigDecimal getSubtotal() { return subtotal; }

        public void setSkuId(String skuId) { this.skuId = skuId; }
        public void setSpuName(String spuName) { this.spuName = spuName; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    }
}
