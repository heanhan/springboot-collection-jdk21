package com.example.ddd.order.interfaces.rest;

import com.example.ddd.common.domain.valueobject.Address;
import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;
import com.example.ddd.common.result.Result;
import com.example.ddd.contract.auth.AuthFeignClient;
import com.example.ddd.contract.order.OrderDTO;
import com.example.ddd.order.application.command.PlaceOrderCommand;
import com.example.ddd.order.application.service.OrderApplicationService;
import com.example.ddd.order.domain.model.aggregate.Order;
import com.example.ddd.order.domain.model.valueobject.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 接口层：鉴别当前用户后转换 Command；订单归属取自 Token，不信任客户端 userId。
 */
@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderApplicationService service;
    private final AuthFeignClient auth;

    public OrderController(OrderApplicationService service, AuthFeignClient auth) {
        this.service = service;
        this.auth = auth;
    }

    /**
     * 请求值对象：价格由商品服务计算，客户端只提供数量和地址。
     */
    public record CreateRequest(@NotEmpty List<@Valid Item> items, @NotNull Address shippingAddress, String remark) {
    }

    /**
     * 请求中的 SKU 明细，无独立生命周期。
     */
    public record Item(@NotBlank String skuId, @Min(1) @Max(999) int quantity) {
    }

    /**
     * 取消原因值对象。
     */
    public record CancelRequest(String reason) {
    }

    @PostMapping
    public Result<String> create(@RequestHeader("Authorization") String token, @Valid @RequestBody CreateRequest request) {
        String user = auth.parseToken(token).requireData().userId();
        return Result.ok(service.placeOrder(new PlaceOrderCommand(user, request.shippingAddress(), request.remark(),
                request.items().stream().map(i -> new PlaceOrderCommand.Item(i.skuId(), i.quantity())).toList())));
    }

    @GetMapping("/{id}")
    public Result<OrderDTO> detail(@RequestHeader("Authorization") String token, @PathVariable String id) {
        Order order = service.getOrder(id);
        if (!order.getUserId().equals(auth.parseToken(token).requireData().userId()))
            throw new BusinessException(ErrorCode.FORBIDDEN);
        return Result.ok(toDTO(order));
    }

    @GetMapping
    public Result<List<OrderDTO>> list(@RequestHeader("Authorization") String token,
                                       @RequestParam(required = false) OrderStatus status, @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return Result.ok(service.listByUser(auth.parseToken(token).requireData().userId(), status, page,
                Math.max(1, Math.min(size, 100))).stream().map(OrderController::toDTO).toList());
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@RequestHeader("Authorization") String token, @PathVariable String id,
                               @RequestBody CancelRequest request) {
        service.cancel(id, auth.parseToken(token).requireData().userId(), "USER_CANCEL", request.reason());
        return Result.ok();
    }

    @PostMapping("/{id}/complete")
    public Result<Void> complete(@RequestHeader("Authorization") String token, @PathVariable String id) {
        service.complete(id, auth.parseToken(token).requireData().userId());
        return Result.ok();
    }

    public static OrderDTO toDTO(Order o) {
        return new OrderDTO(o.getOrderId(), o.getOrderNo(), o.getUserId(), o.getStatus().name(),
                o.getTotalAmount().amount(), o.getShippingFee().amount(), o.getDiscountAmount().amount(), o.getPayAmount().amount(),
                o.getShippingAddress().fullAddress(), o.getShippingAddress().receiver(), o.getShippingAddress().mobile(),
                o.getItems().stream().map(i -> new OrderDTO.OrderItemDTO(i.getItemId(), i.getSkuId(), i.getSpuName(),
                        i.getSkuName(), i.getImage(), i.getUnitPrice().amount(), i.getQuantity(), i.getSubtotal().amount())).toList(), o.getCreatedAt());
    }
}
