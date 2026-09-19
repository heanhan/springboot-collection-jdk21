package com.example.ddd.order;

import com.example.ddd.common.domain.valueobject.*;
import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.order.domain.model.aggregate.Order;
import com.example.ddd.order.domain.model.entity.OrderItem;
import com.example.ddd.order.domain.model.valueobject.OrderStatus;
import com.example.ddd.order.domain.service.OrderPricingService;
import com.example.ddd.order.domain.statemachine.OrderStateMachine;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

/** 订单领域回归：状态机、运费边界、金额快照与非法转换。 */
class OrderDomainTest {
    private Order order() {
        var items=List.of(new OrderItem("item","sku","spu","商品","规格",null,Money.ofCny("99"),1));
        return Order.create("order","no","user",items,new OrderPricingService().price(items),
                new Address("广东","深圳","南山","科技园",null,"用户","13800000000"),null,30);
    }
    @Test void happyPathAndRefund() {
        var o=order();
        assertThat(o.getShippingFee().amount()).isEqualByComparingTo("0");
        o.pay("MOCK","trade",LocalDateTime.now()); o.ship("shipment","MOCK","tracking",null); o.complete(null);
        assertThat(o.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        o.startRefund(); o.finishRefund();
        assertThat(o.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(o.getDomainEvents()).hasSize(4);
    }
    @Test void cannotShipUnpaidOrPayCancelled() {
        var o=order();
        assertThatThrownBy(() -> o.ship("s","MOCK","t",null)).isInstanceOf(BusinessException.class);
        o.cancel("USER_CANCEL",null);
        assertThatThrownBy(() -> o.pay("MOCK","t",null)).isInstanceOf(BusinessException.class);
    }
    @Test void terminalStatesHaveNoOutgoingTransitions() {
        for(var target:OrderStatus.values()) {
            assertThat(OrderStateMachine.canTransition(OrderStatus.CANCELLED,target)).isFalse();
            assertThat(OrderStateMachine.canTransition(OrderStatus.REFUNDED,target)).isFalse();
        }
    }
    @Test void shippingBoundary() {
        var items=List.of(new OrderItem("i","s",null,"n","n",null,Money.ofCny("98.99"),1));
        assertThat(new OrderPricingService().price(items).payAmount().amount()).isEqualByComparingTo("108.99");
    }
}
