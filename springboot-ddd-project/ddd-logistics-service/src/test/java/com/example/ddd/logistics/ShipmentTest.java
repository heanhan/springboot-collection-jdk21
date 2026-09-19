package com.example.ddd.logistics;

import com.example.ddd.logistics.domain.model.aggregate.Shipment;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

/** 物流领域回归：追加轨迹、重复签收和退款拦截。 */
class ShipmentTest {
    private Shipment shipment() { return Shipment.create("s","o","no","u","收件人","13800000000","地址",List.of(new Shipment.Item("sku","商品",1)),Shipment.Carrier.MOCK); }
    @Test void deliveryOnlyOnce() {
        var s=shipment(); s.advance(); s.advance(); s.deliver(); s.deliver();
        assertThat(s.state().status()).isEqualTo(Shipment.Status.DELIVERED);
        assertThat(s.state().tracks()).hasSize(3); assertThat(s.getDomainEvents()).hasSize(2);
    }
    @Test void rejectedShipmentCannotBeDelivered() {
        var s=shipment(); s.reject();
        assertThatThrownBy(s::deliver).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> s.state().items().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
