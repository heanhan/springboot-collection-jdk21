package com.example.ddd.order.interfaces.facade;

import com.example.ddd.common.result.Result;
import com.example.ddd.contract.order.OrderDTO;
import com.example.ddd.order.application.service.OrderApplicationService;
import com.example.ddd.order.interfaces.rest.OrderController;
import org.springframework.web.bind.annotation.*;

/**
 * 接口层：供支付和物流防腐层调用的内网订单摘要。
 */
@RestController
@RequestMapping("/order/internal")
public class OrderInternalFacade {
    private final OrderApplicationService service;

    public OrderInternalFacade(OrderApplicationService service) {
        this.service = service;
    }

    @GetMapping("/{orderId}")
    public Result<OrderDTO> get(@PathVariable String orderId) {
        return Result.ok(OrderController.toDTO(service.getOrder(orderId)));
    }
}
