package com.example.ddd.contract.logistics;

import com.example.ddd.common.result.Result;
import com.example.ddd.contract.ServiceNames;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 物流服务对外 Feign 契约。
 *
 * @author ddd-learning
 */
@FeignClient(name = ServiceNames.LOGISTICS, contextId = "logisticsFeignClient", path = "/logistics/internal", url = "${ddd.services.logistics-url:http://localhost:8087}")
public interface LogisticsFeignClient {

    /**
     * 根据订单 ID 查询发货单（一个订单可能对应多个发货单，例如拆包发货）。
     */
    @GetMapping("/by-order/{orderId}")
    Result<List<ShipmentDTO>> listByOrderId(@PathVariable("orderId") String orderId);

    /**
     * 根据发货单 ID 查询详情。
     */
    @GetMapping("/{shipmentId}")
    Result<ShipmentDTO> getById(@PathVariable("shipmentId") String shipmentId);
}
