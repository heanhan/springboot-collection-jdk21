package com.example.ddd.contract.order;

import com.example.ddd.common.result.Result;
import com.example.ddd.contract.ServiceNames;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 订单服务对外 Feign 契约。
 *
 * <p>主要供支付、物流服务查询订单基本信息。</p>
 *
 * @author ddd-learning
 */
@FeignClient(name = ServiceNames.ORDER, contextId = "orderFeignClient", path = "/order/internal",
        url = "${ddd.services.order-url:}")
public interface OrderFeignClient {

    /**
     * 根据 orderId 查询订单摘要。
     */
    @GetMapping("/{orderId}")
    Result<OrderDTO> getById(@PathVariable("orderId") String orderId);
}
