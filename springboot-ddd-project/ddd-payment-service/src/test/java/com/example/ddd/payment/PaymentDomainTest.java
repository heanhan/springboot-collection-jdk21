package com.example.ddd.payment;

import com.example.ddd.payment.domain.model.aggregate.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;

/** 支付领域回归：金额/状态校验、重复回调、全额退款与过期支付。 */
class PaymentDomainTest {
    private PaymentOrder payment() { return PaymentOrder.create("p","o","no","u",BigDecimal.TEN,LocalDateTime.now().plusMinutes(30)); }
    @Test void callbackAndRefundAreIdempotent() {
        var p=payment();
        p.confirm(new BigDecimal("10.00"),"trade",LocalDateTime.now()); p.confirm(BigDecimal.TEN,"trade",LocalDateTime.now());
        assertThat(p.getDomainEvents()).hasSize(1);
        assertThatThrownBy(() -> p.confirm(BigDecimal.TEN,"other",LocalDateTime.now())).isInstanceOf(RuntimeException.class);
        var r=RefundOrder.create("r",p,"退款"); p.refund(); p.refund(); r.succeed(); r.succeed();
        assertThat(r.getDomainEvents()).hasSize(1); assertThat(p.state().status()).isEqualTo(PaymentOrder.Status.REFUNDED);
    }
    @Test void rejectsWrongAmountAndClosedPayments() {
        var p=payment();
        assertThatThrownBy(() -> p.confirm(BigDecimal.ONE,"trade",LocalDateTime.now())).isInstanceOf(RuntimeException.class);
        p.close(); assertThatThrownBy(() -> p.confirm(BigDecimal.TEN,"trade",LocalDateTime.now())).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(p::refund).isInstanceOf(RuntimeException.class);
    }
    @Test void rejectsExpiredPayment() {
        var p=PaymentOrder.create("p","o","no","u",BigDecimal.TEN,LocalDateTime.now().minusSeconds(1));
        assertThatThrownBy(() -> p.confirm(BigDecimal.TEN,"trade",LocalDateTime.now())).isInstanceOf(RuntimeException.class);
    }
}
