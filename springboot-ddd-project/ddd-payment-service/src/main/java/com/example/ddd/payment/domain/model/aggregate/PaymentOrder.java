package com.example.ddd.payment.domain.model.aggregate;

import com.example.ddd.common.domain.model.BaseAggregateRoot;
import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;
import com.example.ddd.contract.payment.event.PaymentSuccessEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 领域层支付聚合：一订单一支付单；金额固定，仅待支付单可收款；成功回调按流水幂等。
 */
public class PaymentOrder extends BaseAggregateRoot {
    /**
     * 渠道值对象：无标识、按枚举值相等。所有渠道均为模拟器。
     */
    public enum Channel {ALIPAY, WECHAT, MOCK}

    /**
     * 支付生命周期；全额退款后终结。
     */
    public enum Status {PENDING, SUCCESS, CLOSED, REFUNDED}

    /**
     * 不可变状态快照，供仓储重建使用，不含框架类型。
     */
    public record State(String paymentId, String orderId, String orderNo, String userId, BigDecimal amount,
                        Channel channel, Status status, String tradeNo, LocalDateTime createdAt,
                        LocalDateTime paidAt, LocalDateTime expireAt) {
    }

    private State state;

    private PaymentOrder(State state) {
        this.state = state;
    }

    public static PaymentOrder restore(State state) {
        return new PaymentOrder(state);
    }

    public static PaymentOrder create(String id, String orderId, String orderNo, String userId,
                                      BigDecimal amount, LocalDateTime expireAt) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("支付金额必须大于零");
        return new PaymentOrder(new State(id, orderId, orderNo, userId, amount, Channel.MOCK, Status.PENDING,
                null, LocalDateTime.now(), null, Objects.requireNonNull(expireAt)));
    }

    public State state() {
        return state;
    }

    @Override
    public String aggregateId() {
        return state.paymentId();
    }

    public void selectChannel(Channel channel) {
        require(Status.PENDING);
        replace(Objects.requireNonNull(channel), state.status(), state.tradeNo(), state.paidAt());
    }

    public void confirm(BigDecimal amount, String tradeNo, LocalDateTime now) {
        if (amount == null || amount.compareTo(state.amount()) != 0) fail("回调金额不符");
        if (tradeNo == null || tradeNo.isBlank()) fail("支付流水不能为空");
        if (state.status() == Status.SUCCESS || state.status() == Status.REFUNDED) {
            if (!tradeNo.equals(state.tradeNo())) fail("支付流水冲突");
            return;
        }
        require(Status.PENDING);
        if (!now.isBefore(state.expireAt())) fail("支付单已过期");
        replace(state.channel(), Status.SUCCESS, tradeNo, now);
        registerEvent(new PaymentSuccessEvent(state.paymentId(), state.orderId(), state.orderNo(), state.userId(),
                state.amount(), state.channel().name(), tradeNo, now));
    }

    public void close() {
        if (state.status() == Status.CLOSED) return;
        require(Status.PENDING);
        replace(state.channel(), Status.CLOSED, state.tradeNo(), state.paidAt());
    }

    public void refund() {
        if (state.status() == Status.REFUNDED) return;
        require(Status.SUCCESS);
        replace(state.channel(), Status.REFUNDED, state.tradeNo(), state.paidAt());
    }

    private void replace(Channel channel, Status status, String tradeNo, LocalDateTime paidAt) {
        state = new State(state.paymentId(), state.orderId(), state.orderNo(), state.userId(), state.amount(),
                channel, status, tradeNo, state.createdAt(), paidAt, state.expireAt());
    }

    private void require(Status expected) {
        if (state.status() != expected) fail("支付状态不允许此操作: " + state.status());
    }

    private void fail(String message) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, message);
    }
}
