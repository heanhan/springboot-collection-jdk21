package com.example.ddd.payment.domain.model.aggregate;

import com.example.ddd.common.domain.model.BaseAggregateRoot;
import com.example.ddd.contract.payment.event.RefundSuccessEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 领域退款聚合：当前仅全额退款，一支付单最多一个退款单，成功后不可修改。
 */
public class RefundOrder extends BaseAggregateRoot {
    /**
     * 退款状态快照：不可变且可重建。
     */
    public record State(String refundId, String paymentId, String orderId, BigDecimal amount,
                        String reason, String status, LocalDateTime createdAt) {
    }

    private State state;

    private RefundOrder(State state) {
        this.state = state;
    }

    public static RefundOrder restore(State state) {
        return new RefundOrder(state);
    }

    public static RefundOrder create(String id, PaymentOrder payment, String reason) {
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("退款原因不能为空");
        var p = payment.state();
        return new RefundOrder(new State(id, p.paymentId(), p.orderId(), p.amount(), reason, "PENDING", LocalDateTime.now()));
    }

    public void succeed() {
        if ("SUCCESS".equals(state.status())) return;
        state = new State(state.refundId(), state.paymentId(), state.orderId(), state.amount(), state.reason(), "SUCCESS", state.createdAt());
        registerEvent(new RefundSuccessEvent(state.refundId(), state.orderId(), state.amount()));
    }

    public State state() {
        return state;
    }

    @Override
    public String aggregateId() {
        return state.refundId();
    }
}
