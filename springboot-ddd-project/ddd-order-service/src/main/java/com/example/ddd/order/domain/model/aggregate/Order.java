package com.example.ddd.order.domain.model.aggregate;

import com.example.ddd.common.domain.model.BaseAggregateRoot;
import com.example.ddd.common.domain.valueobject.Address;
import com.example.ddd.common.domain.valueobject.Money;
import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;
import com.example.ddd.contract.order.event.OrderCancelledEvent;
import com.example.ddd.contract.order.event.OrderCompletedEvent;
import com.example.ddd.contract.order.event.OrderCreatedEvent;
import com.example.ddd.contract.order.event.OrderPaidEvent;
import com.example.ddd.contract.order.event.OrderShippedEvent;
import com.example.ddd.order.domain.model.entity.OrderItem;
import com.example.ddd.order.domain.model.valueobject.OrderStatus;
import com.example.ddd.order.domain.service.OrderPricingService.PricingResult;
import com.example.ddd.order.domain.statemachine.OrderStateMachine;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 聚合根：Order 订单。
 *
 * <p><b>聚合边界：</b>Order（聚合根）+ OrderItem（子实体）+ Money/Address（值对象）。
 * 外部只能通过 Order 访问 OrderItem，一次事务只修改一个 Order 实例。</p>
 *
 * <p><b>不变式 (Invariants)：</b>
 * <ol>
 *   <li>订单项非空（至少一条）。</li>
 *   <li>payAmount == totalAmount + shippingFee - discountAmount。</li>
 *   <li>状态流转必须经 {@link OrderStateMachine} 校验，禁止跳跃 / 回退。</li>
 *   <li>金额快照、地址快照一经创建不可变。</li>
 * </ol>
 *
 * <p><b>生命周期：</b>CREATED → (PAID → SHIPPED → COMPLETED) | CANCELLED | (REFUNDING → REFUNDED)。</p>
 *
 * <p><b>事件收集：</b>每次状态变更通过 {@code registerEvent} 暂存"集成事件"（发布语言位于 ddd-api-contract），
 * 由应用服务在事务提交后统一发布，领域层本身不依赖 MQ。</p>
 */
public class Order extends BaseAggregateRoot {

    private final String orderId;
    private final String orderNo;
    private final String userId;
    private final List<OrderItem> items;
    private final Money totalAmount;
    private final Money shippingFee;
    private final Money discountAmount;
    private final Money payAmount;
    private final Address shippingAddress;
    private final String remark;
    private final LocalDateTime createdAt;
    private final LocalDateTime expireAt;

    private OrderStatus status;
    private String cancelReason;
    private String paymentChannel;
    private String paymentTradeNo;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

    // ============================================================
    // 工厂方法
    // ============================================================

    /**
     * 创建订单（下单用例）。校验不变式并收集 {@link OrderCreatedEvent}。
     *
     * @param pricing             由 {@code OrderPricingService} 计算好的金额结果
     * @param paymentTimeoutMinutes 支付超时分钟数，用于计算 expireAt
     */
    public static Order create(String orderId, String orderNo, String userId, List<OrderItem> items,
                               PricingResult pricing, Address shippingAddress, String remark,
                               int paymentTimeoutMinutes) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_ITEM_EMPTY);
        }
        Objects.requireNonNull(pricing, "pricing 不能为 null");
        Objects.requireNonNull(shippingAddress, "收货地址不能为 null");

        Order order = new Order(orderId, orderNo, userId, items, pricing, shippingAddress, remark,
                LocalDateTime.now(), LocalDateTime.now().plusMinutes(paymentTimeoutMinutes));
        order.status = OrderStatus.CREATED;

        // 收集集成事件：订单已创建
        List<OrderCreatedEvent.Item> eventItems = new ArrayList<>();
        for (OrderItem item : items) {
            eventItems.add(new OrderCreatedEvent.Item(item.getSkuId(), item.getSpuName(),
                    item.getQuantity(), item.getSubtotal().amount()));
        }
        order.registerEvent(new OrderCreatedEvent(orderId, orderNo, userId,
                pricing.payAmount().amount(), eventItems, order.expireAt));
        return order;
    }

    /**
     * 重建订单（从持久化恢复）。不触发校验、不收集事件。
     */
    public static Order reconstitute(String orderId, String orderNo, String userId, List<OrderItem> items,
                                     Money totalAmount, Money shippingFee, Money discountAmount, Money payAmount,
                                     Address shippingAddress, String remark, OrderStatus status,
                                     String cancelReason, String paymentChannel, String paymentTradeNo,
                                     LocalDateTime createdAt, LocalDateTime expireAt, LocalDateTime paidAt,
                                     LocalDateTime shippedAt, LocalDateTime completedAt, LocalDateTime cancelledAt) {
        PricingResult pricing = new PricingResult(totalAmount, shippingFee, discountAmount, payAmount);
        Order order = new Order(orderId, orderNo, userId, items, pricing, shippingAddress, remark, createdAt, expireAt);
        order.status = status;
        order.cancelReason = cancelReason;
        order.paymentChannel = paymentChannel;
        order.paymentTradeNo = paymentTradeNo;
        order.paidAt = paidAt;
        order.shippedAt = shippedAt;
        order.completedAt = completedAt;
        order.cancelledAt = cancelledAt;
        return order;
    }

    private Order(String orderId, String orderNo, String userId, List<OrderItem> items,
                  PricingResult pricing, Address shippingAddress, String remark,
                  LocalDateTime createdAt, LocalDateTime expireAt) {
        this.orderId = Objects.requireNonNull(orderId);
        this.orderNo = Objects.requireNonNull(orderNo);
        this.userId = Objects.requireNonNull(userId);
        this.items = List.copyOf(items);
        this.totalAmount = pricing.totalAmount();
        this.shippingFee = pricing.shippingFee();
        this.discountAmount = pricing.discountAmount();
        this.payAmount = pricing.payAmount();
        // 不变式 2：实付金额必须等于 total + shipping - discount
        Money expected = this.totalAmount.add(this.shippingFee).subtract(this.discountAmount);
        if (expected.amount().compareTo(this.payAmount.amount()) != 0) {
            throw new BusinessException(ErrorCode.ORDER_AMOUNT_MISMATCH,
                    "实付金额与明细不符: 期望=" + expected + " 实际=" + this.payAmount);
        }
        this.shippingAddress = shippingAddress;
        this.remark = remark;
        this.createdAt = createdAt;
        this.expireAt = expireAt;
    }

    // ============================================================
    // 业务行为（每个都先过状态机，再改状态，最后收集事件）
    // ============================================================

    /**
     * 支付成功：CREATED → PAID。收集 {@link OrderPaidEvent}。
     */
    public void pay(String channel, String tradeNo, LocalDateTime paidTime) {
        OrderStateMachine.assertCanTransition(this.status, OrderStatus.PAID);
        this.status = OrderStatus.PAID;
        this.paymentChannel = channel;
        this.paymentTradeNo = tradeNo;
        this.paidAt = paidTime == null ? LocalDateTime.now() : paidTime;
        registerEvent(new OrderPaidEvent(orderId, orderNo, userId, payAmount.amount(),
                channel, tradeNo, this.paidAt));
    }

    /**
     * 发货：PAID → SHIPPED。收集 {@link OrderShippedEvent}。
     */
    public void ship(String shipmentId, String carrier, String trackingNo, LocalDateTime shipTime) {
        OrderStateMachine.assertCanTransition(this.status, OrderStatus.SHIPPED);
        this.status = OrderStatus.SHIPPED;
        this.shippedAt = shipTime == null ? LocalDateTime.now() : shipTime;
        registerEvent(new OrderShippedEvent(orderId, orderNo, userId, shipmentId, carrier, trackingNo, this.shippedAt));
    }

    /**
     * 确认收货：SHIPPED → COMPLETED。收集 {@link OrderCompletedEvent}。
     */
    public void complete(LocalDateTime completeTime) {
        OrderStateMachine.assertCanTransition(this.status, OrderStatus.COMPLETED);
        this.status = OrderStatus.COMPLETED;
        this.completedAt = completeTime == null ? LocalDateTime.now() : completeTime;
        registerEvent(new OrderCompletedEvent(orderId, orderNo, userId, this.completedAt));
    }

    /**
     * 取消：CREATED → CANCELLED。收集 {@link OrderCancelledEvent}。
     *
     * @param reasonCode USER_CANCEL / TIMEOUT / PAY_FAILED / OUT_OF_STOCK
     */
    public void cancel(String reasonCode, String reasonDesc) {
        OrderStateMachine.assertCanTransition(this.status, OrderStatus.CANCELLED);
        this.status = OrderStatus.CANCELLED;
        this.cancelReason = reasonCode + (reasonDesc == null ? "" : ":" + reasonDesc);
        this.cancelledAt = LocalDateTime.now();
        registerEvent(new OrderCancelledEvent(orderId, orderNo, userId, reasonCode, reasonDesc));
    }

    /**
     * 申请退款：PAID/SHIPPED → REFUNDING。
     */
    public void startRefund() {
        OrderStateMachine.assertCanTransition(this.status, OrderStatus.REFUNDING);
        this.status = OrderStatus.REFUNDING;
    }

    /**
     * 退款完成：REFUNDING → REFUNDED。
     */
    public void finishRefund() {
        OrderStateMachine.assertCanTransition(this.status, OrderStatus.REFUNDED);
        this.status = OrderStatus.REFUNDED;
    }

    // ============================================================
    // 查询行为
    // ============================================================

    /** 是否已超时未支付（供定时关单任务判断）。 */
    public boolean isPaymentExpired() {
        return this.status == OrderStatus.CREATED && LocalDateTime.now().isAfter(this.expireAt);
    }

    @Override
    public String aggregateId() {
        return orderId;
    }

    public String getOrderId() { return orderId; }
    public String getOrderNo() { return orderNo; }
    public String getUserId() { return userId; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
    public Money getTotalAmount() { return totalAmount; }
    public Money getShippingFee() { return shippingFee; }
    public Money getDiscountAmount() { return discountAmount; }
    public Money getPayAmount() { return payAmount; }
    public Address getShippingAddress() { return shippingAddress; }
    public String getRemark() { return remark; }
    public OrderStatus getStatus() { return status; }
    public String getCancelReason() { return cancelReason; }
    public String getPaymentChannel() { return paymentChannel; }
    public String getPaymentTradeNo() { return paymentTradeNo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpireAt() { return expireAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public LocalDateTime getShippedAt() { return shippedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
}
