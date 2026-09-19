package com.example.ddd.payment.domain.service;

import com.example.ddd.payment.domain.model.aggregate.PaymentOrder;
import com.example.ddd.payment.domain.model.aggregate.RefundOrder;
import java.util.Map;

/** 领域策略服务：渠道行为跨支付/退款聚合，通过适配器端口隔离外部支付系统。 */
public class PaymentChannelService {
    /** 外部渠道端口，所有本项目实现均为 Mock，不产生真实资金流动。 */
    public interface Adapter {
        String payUrl(PaymentOrder.State payment);
        void refund(RefundOrder.State refund);
    }
    private final Map<PaymentOrder.Channel, Adapter> adapters;
    public PaymentChannelService(Map<PaymentOrder.Channel, Adapter> adapters) { this.adapters = Map.copyOf(adapters); }
    public String payUrl(PaymentOrder payment) { return adapters.get(payment.state().channel()).payUrl(payment.state()); }
    public void refund(PaymentOrder payment, RefundOrder refund) { adapters.get(payment.state().channel()).refund(refund.state()); }
}
