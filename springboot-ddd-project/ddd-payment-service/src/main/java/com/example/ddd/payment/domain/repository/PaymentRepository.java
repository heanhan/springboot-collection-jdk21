package com.example.ddd.payment.domain.repository;

import com.example.ddd.payment.domain.model.aggregate.*;
import java.util.Optional;

/** 领域仓储端口：支付写用例加行锁，退款与支付同事务保存，避免重复退款。 */
public interface PaymentRepository {
    Optional<PaymentOrder> find(String id, boolean forUpdate);
    Optional<PaymentOrder> findByOrder(String orderId);
    Optional<RefundOrder> findRefund(String paymentId);
    void save(PaymentOrder payment);
    void save(RefundOrder refund);
}
