package com.example.ddd.logistics.application.port;

import com.example.ddd.common.domain.event.DomainEvent;
import com.example.ddd.contract.order.OrderDTO;

/** 应用输出端口：订单快照、支付状态、库存实扣和可靠事件发布。 */
public final class FulfillmentPorts {
    private FulfillmentPorts() {}
    /** 外部业务防腐层，不暴露 Feign/HTTP 类型。 */
    public interface Gateway {
        OrderDTO order(String id);
        boolean refunded(String orderId);
        void deduct(String orderId);
    }
    /** 事件发布端口。 */
    public interface Events { void publish(DomainEvent event); }
}
