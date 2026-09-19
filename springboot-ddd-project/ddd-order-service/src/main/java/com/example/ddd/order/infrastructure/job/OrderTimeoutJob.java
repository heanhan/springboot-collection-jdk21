package com.example.ddd.order.infrastructure.job;

import com.example.ddd.order.application.service.OrderApplicationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 基础设施定时触发器：逐单独立事务，单笔失败不阻断其他超时订单。 */
@Component
public class OrderTimeoutJob {
    private final OrderApplicationService service;
    public OrderTimeoutJob(OrderApplicationService service) { this.service = service; }
    @Scheduled(fixedDelayString = "${ddd.order.timeout-scan-interval-ms:60000}")
    public void closeExpired() {
        for (var order : service.findTimeoutOrders(100)) {
            try { service.cancel(order.getOrderId(), null, "TIMEOUT", "超过支付期限"); }
            catch (Exception e) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("关单失败 orderId={}", order.getOrderId(), e); }
        }
    }
}
