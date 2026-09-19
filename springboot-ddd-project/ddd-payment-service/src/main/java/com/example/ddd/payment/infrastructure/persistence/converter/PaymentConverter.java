package com.example.ddd.payment.infrastructure.persistence.converter;

import com.example.ddd.payment.domain.model.aggregate.*;
import com.example.ddd.payment.infrastructure.persistence.po.*;

/**
 * 基础设施转换器：显式映射 PO/领域快照；重建不会再次产生事件。
 */
public final class PaymentConverter {
    private PaymentConverter() {
    }

    public static PaymentOrder domain(PaymentPO p) {
        return PaymentOrder.restore(new PaymentOrder.State(p.paymentId, p.orderId, p.orderNo, p.userId, p.amount,
                PaymentOrder.Channel.valueOf(p.channel), PaymentOrder.Status.valueOf(p.status), p.tradeNo, p.createdAt, p.paidAt, p.expireAt));
    }

    public static void copy(PaymentOrder value, PaymentPO p) {
        var s = value.state();
        p.paymentId = s.paymentId();
        p.orderId = s.orderId();
        p.orderNo = s.orderNo();
        p.userId = s.userId();
        p.amount = s.amount();
        p.channel = s.channel().name();
        p.status = s.status().name();
        p.tradeNo = s.tradeNo();
        p.createdAt = s.createdAt();
        p.paidAt = s.paidAt();
        p.expireAt = s.expireAt();
    }

    public static RefundOrder domain(RefundPO p) {
        return RefundOrder.restore(new RefundOrder.State(p.refundId, p.paymentId, p.orderId, p.amount, p.reason, p.status, p.createdAt));
    }

    public static RefundPO po(RefundOrder value) {
        var s = value.state();
        var p = new RefundPO();
        p.refundId = s.refundId();
        p.paymentId = s.paymentId();
        p.orderId = s.orderId();
        p.amount = s.amount();
        p.reason = s.reason();
        p.status = s.status();
        p.createdAt = s.createdAt();
        return p;
    }
}
