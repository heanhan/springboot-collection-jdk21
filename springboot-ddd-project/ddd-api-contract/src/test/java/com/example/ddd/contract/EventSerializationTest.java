package com.example.ddd.contract;

import com.example.ddd.contract.payment.event.PaymentSuccessEvent;
import com.example.ddd.contract.order.event.OrderPaidEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;

/** 集成契约测试：反序列化必须恢复原 eventId，而不是生成新事件或因空标识失败。 */
class EventSerializationTest {
    private final ObjectMapper json=new ObjectMapper().findAndRegisterModules();
    @Test void paymentRoundTripPreservesIdentity() throws Exception {
        var original=new PaymentSuccessEvent("payment","order","no","user",new BigDecimal("12.00"),"MOCK","trade",LocalDateTime.now());
        var copy=json.readValue(json.writeValueAsBytes(original),PaymentSuccessEvent.class);
        assertThat(copy.eventId()).isEqualTo(original.eventId());
        assertThat(copy.aggregateId()).isEqualTo("payment");
        assertThat(copy.occurredOn()).isEqualTo(original.occurredOn());
        assertThat(copy.getAmount()).isEqualByComparingTo("12.00");
    }
    @Test void orderRoundTripPreservesFields() throws Exception {
        var original=new OrderPaidEvent("order","no","user",BigDecimal.TEN,"MOCK","trade",LocalDateTime.now());
        var copy=json.readValue(json.writeValueAsBytes(original),OrderPaidEvent.class);
        assertThat(copy.eventId()).isEqualTo(original.eventId());
        assertThat(copy.aggregateId()).isEqualTo("order");
    }
}
