package com.example.ddd.contract.payment;

import com.example.ddd.common.result.Result;
import com.example.ddd.contract.ServiceNames;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 支付服务对外 Feign 契约。
 *
 * @author ddd-learning
 */
@FeignClient(name = ServiceNames.PAYMENT, contextId = "paymentFeignClient", path = "/payment/internal", url = "${ddd.services.payment-url:http://localhost:8086}")
public interface PaymentFeignClient {

    /**
     * 根据 orderId 查询支付单信息。
     */
    @GetMapping("/by-order/{orderId}")
    Result<PaymentOrderDTO> getByOrderId(@PathVariable("orderId") String orderId);

    /**
     * 根据 paymentId 查询支付单。
     */
    @GetMapping("/{paymentId}")
    Result<PaymentOrderDTO> getById(@PathVariable("paymentId") String paymentId);
}
