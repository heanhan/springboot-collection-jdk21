package com.example.ddd.payment.interfaces.rest;

import com.example.ddd.common.exception.*;
import com.example.ddd.common.result.Result;
import com.example.ddd.contract.auth.AuthFeignClient;
import com.example.ddd.contract.payment.PaymentOrderDTO;
import com.example.ddd.payment.application.service.PayApplicationService;
import com.example.ddd.payment.domain.model.aggregate.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 接口层：用户支付、退款和仅用于学习的认证 Mock 回调；真实渠道必须替换为验签回调。
 */
@RestController
@RequestMapping("/payment")
public class PaymentController {
    private final PayApplicationService service;
    private final AuthFeignClient auth;
    private final boolean mockEnabled;

    public PaymentController(PayApplicationService service, AuthFeignClient auth, @Value("${ddd.payment.mock-enabled:false}") boolean mockEnabled) {
        this.service = service;
        this.auth = auth;
        this.mockEnabled = mockEnabled;
    }

    /**
     * 渠道选择请求。
     */
    public record PayRequest(@NotNull PaymentOrder.Channel channel) {
    }

    /**
     * 模拟回调请求，金额与流水也要通过领域校验。
     */
    public record Callback(@NotBlank String paymentId, @NotNull @DecimalMin("0.01") BigDecimal amount,
                           @NotBlank String tradeNo) {
    }

    /**
     * 全额退款原因。
     */
    public record RefundRequest(@NotBlank @Size(max = 255) String reason) {
    }

    @GetMapping("/by-order/{orderId}")
    public Result<PaymentOrderDTO> byOrder(@PathVariable String orderId, @RequestHeader("Authorization") String token) {
        var p = service.byOrder(orderId);
        if (!p.userId().equals(auth.parseToken(token).requireData().userId()))
            throw new BusinessException(ErrorCode.FORBIDDEN);
        return Result.ok(dto(p));
    }

    @PostMapping("/{id}/pay")
    public Result<String> pay(@PathVariable String id, @RequestHeader("Authorization") String token, @Valid @RequestBody PayRequest body) {
        return Result.ok(service.pay(id, auth.parseToken(token).requireData().userId(), body.channel()));
    }

    @PostMapping("/mock/callback")
    public Result<Void> callback(@RequestHeader("Authorization") String token, @Valid @RequestBody Callback body) {
        if (!mockEnabled) throw new BusinessException(ErrorCode.FORBIDDEN, "Mock 回调未启用");
        service.callback(body.paymentId(), auth.parseToken(token).requireData().userId(), body.amount(), body.tradeNo());
        return Result.ok();
    }

    @PostMapping("/{id}/refund")
    public Result<RefundOrder.State> refund(@PathVariable String id, @RequestHeader("Authorization") String token, @Valid @RequestBody RefundRequest body) {
        return Result.ok(service.refund(id, auth.parseToken(token).requireData().userId(), body.reason()));
    }

    @GetMapping("/internal/by-order/{orderId}")
    public Result<PaymentOrderDTO> internalOrder(@PathVariable String orderId) {
        return Result.ok(dto(service.byOrder(orderId)));
    }

    @GetMapping("/internal/{id}")
    public Result<PaymentOrderDTO> internal(@PathVariable String id) {
        return Result.ok(dto(service.get(id)));
    }

    private PaymentOrderDTO dto(PaymentOrder.State p) {
        return new PaymentOrderDTO(p.paymentId(), p.orderId(), p.orderNo(), p.userId(), p.amount(), p.channel().name(), p.status().name(), p.tradeNo(), p.paidAt(), p.createdAt());
    }
}
