package com.example.ddd.payment.infrastructure.persistence.repository;

import com.example.ddd.payment.domain.model.aggregate.*;
import com.example.ddd.payment.domain.repository.PaymentRepository;
import com.example.ddd.payment.infrastructure.persistence.po.*;
import com.example.ddd.payment.infrastructure.persistence.converter.PaymentConverter;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA 仓储：单行加载完整聚合；写入复用事务内受管 PO 的版本，行锁由应用事务持有。
 */
@Repository
public class PaymentRepositoryImpl implements PaymentRepository {
    @PersistenceContext
    private EntityManager em;

    public Optional<PaymentOrder> find(String id, boolean forUpdate) {
        return Optional.ofNullable(forUpdate ? em.find(PaymentPO.class, id, LockModeType.PESSIMISTIC_WRITE) : em.find(PaymentPO.class, id)).map(PaymentConverter::domain);
    }

    public Optional<PaymentOrder> findByOrder(String orderId) {
        return em.createQuery("from PaymentPO p where p.orderId=:id", PaymentPO.class).setParameter("id", orderId)
                .getResultStream().findFirst().map(PaymentConverter::domain);
    }

    public Optional<RefundOrder> findRefund(String paymentId) {
        return em.createQuery("from RefundPO r where r.paymentId=:id", RefundPO.class).setParameter("id", paymentId)
                .getResultStream().findFirst().map(PaymentConverter::domain);
    }

    public void save(PaymentOrder payment) {
        var po = em.find(PaymentPO.class, payment.aggregateId());
        if (po == null) {
            po = new PaymentPO();
            PaymentConverter.copy(payment, po);
            em.persist(po);
        } else PaymentConverter.copy(payment, po);
    }

    public void save(RefundOrder refund) {
        em.persist(PaymentConverter.po(refund));
    }
}
