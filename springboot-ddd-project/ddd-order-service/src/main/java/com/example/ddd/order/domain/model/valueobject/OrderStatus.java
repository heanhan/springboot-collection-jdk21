package com.example.ddd.order.domain.model.valueobject;

/**
 * 值对象：订单状态 (OrderStatus)。
 *
 * <p><b>为什么是枚举值对象？</b>状态是有限、可枚举、无独立标识的——完全符合值对象定义。
 * 状态之间的合法转换由 {@code OrderStateMachine} 守护，而不是散落在各处的 if-else。</p>
 *
 * <p><b>状态流转全景：</b>
 * <pre>
 *   CREATED ──pay──▶ PAID ──ship──▶ SHIPPED ──complete──▶ COMPLETED
 *      │              │                │
 *      │cancel        │refund          │refund
 *      ▼              ▼                ▼
 *   CANCELLED     REFUNDING ───────▶ REFUNDED
 * </pre>
 */
public enum OrderStatus {

    /** 已创建，待支付 */
    CREATED,
    /** 已支付，待发货 */
    PAID,
    /** 已发货，待收货 */
    SHIPPED,
    /** 已完成（用户确认收货） */
    COMPLETED,
    /** 已取消（用户取消 / 超时关单） */
    CANCELLED,
    /** 退款中 */
    REFUNDING,
    /** 已退款 */
    REFUNDED;

    /** 是否为终态（不可再流转）。 */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == REFUNDED;
    }

    /** 是否可支付。 */
    public boolean isPayable() {
        return this == CREATED;
    }

    /** 是否可取消（仅未支付可取消）。 */
    public boolean isCancellable() {
        return this == CREATED;
    }
}
