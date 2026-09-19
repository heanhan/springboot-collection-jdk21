package com.example.ddd.logistics.interfaces.rest;

import com.example.ddd.common.exception.*;
import com.example.ddd.common.result.Result;
import com.example.ddd.contract.auth.AuthFeignClient;
import com.example.ddd.contract.logistics.ShipmentDTO;
import com.example.ddd.logistics.application.service.ShipApplicationService;
import com.example.ddd.logistics.domain.model.aggregate.Shipment;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** 接口层：用户查询与签收校验归属，内部契约仅用于可信服务网络。 */
@RestController
@RequestMapping("/logistics")
public class ShipmentController {
    private final ShipApplicationService service; private final AuthFeignClient auth;
    public ShipmentController(ShipApplicationService service,AuthFeignClient auth) { this.service=service; this.auth=auth; }
    @GetMapping("/by-order/{id}")
    public Result<List<ShipmentDTO>> byOrder(@PathVariable String id,@RequestHeader("Authorization") String token) {
        String user=auth.parseToken(token).requireData().userId();
        var shipments=service.byOrder(id);
        if(shipments.stream().anyMatch(s -> !s.userId().equals(user))) throw new BusinessException(ErrorCode.FORBIDDEN);
        return Result.ok(shipments.stream().map(this::dto).toList());
    }
    @GetMapping("/{id}")
    public Result<ShipmentDTO> get(@PathVariable String id,@RequestHeader("Authorization") String token) {
        var s=service.get(id);
        if(!s.userId().equals(auth.parseToken(token).requireData().userId())) throw new BusinessException(ErrorCode.FORBIDDEN);
        return Result.ok(dto(s));
    }
    @PostMapping("/{id}/deliver")
    public Result<Void> deliver(@PathVariable String id,@RequestHeader("Authorization") String token) {
        service.deliver(id,auth.parseToken(token).requireData().userId()); return Result.ok();
    }
    @GetMapping("/internal/by-order/{id}")
    public Result<List<ShipmentDTO>> internalOrder(@PathVariable String id) { return Result.ok(service.byOrder(id).stream().map(this::dto).toList()); }
    @GetMapping("/internal/{id}")
    public Result<ShipmentDTO> internal(@PathVariable String id) { return Result.ok(dto(service.get(id))); }
    private ShipmentDTO dto(Shipment.State s) {
        return new ShipmentDTO(s.shipmentId(),s.orderId(),s.orderNo(),s.userId(),s.carrier().name(),s.trackingNo(),s.status().name(),
                s.receiver(),s.mobile(),s.address(),s.shippedAt(),s.deliveredAt(),s.tracks().stream()
                .map(t -> new ShipmentDTO.Track(t.time(),t.location(),t.description())).toList());
    }
}
