package com.example.ddd.payment.application.port;

import com.example.ddd.common.domain.event.DomainEvent;
import com.example.ddd.contract.order.OrderDTO;

/** 应用层输出端口：隔离 RPC 和 MQ 技术，DTO 是上下文间已发布的语言。 */
public final class PaymentPorts {
    private PaymentPorts() {}
    /** 订单查询防腐层。 */
    public interface Orders { OrderDTO get(String orderId); }
    /** 可靠事件发布端口。 */
    public interface Events { void publish(DomainEvent event); }
}
