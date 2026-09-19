package com.example.ddd.cart.interfaces.rest;

import com.example.ddd.cart.application.service.CartApplicationService;
import com.example.ddd.common.result.Result;
import com.example.ddd.contract.auth.AuthFeignClient;
import com.example.ddd.contract.cart.CartItemDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** 接口层：所有用户操作从认证服务取得身份，禁止用户指定其他购物车的 userId。 */
@RestController
@RequestMapping("/cart")
public class CartController {
    private final CartApplicationService service; private final AuthFeignClient auth;
    public CartController(CartApplicationService service,AuthFeignClient auth) { this.service=service; this.auth=auth; }
    /** 加购请求值对象。 */
    public record Add(@NotBlank String skuId,@Min(1) @Max(999) int quantity) {}
    /** 改量和选中请求值对象。 */
    public record Change(@Min(1) @Max(999) int quantity,boolean checked) {}
    private String user(String token) { return auth.parseToken(token).requireData().userId(); }
    @PostMapping("/items")
    public Result<Void> add(@RequestHeader("Authorization") String token,@Valid @RequestBody Add body) {
        service.add(user(token),body.skuId(),body.quantity()); return Result.ok();
    }
    @PutMapping("/items/{sku}")
    public Result<Void> change(@RequestHeader("Authorization") String token,@PathVariable String sku,@Valid @RequestBody Change body) {
        service.change(user(token),sku,body.quantity(),body.checked()); return Result.ok();
    }
    @DeleteMapping("/items/{sku}")
    public Result<Void> remove(@RequestHeader("Authorization") String token,@PathVariable String sku) { service.remove(user(token),sku); return Result.ok(); }
    @DeleteMapping("/items")
    public Result<Void> clear(@RequestHeader("Authorization") String token) { service.clear(user(token)); return Result.ok(); }
    @GetMapping("/items")
    public Result<List<CartItemDTO>> list(@RequestHeader("Authorization") String token) { return Result.ok(service.list(user(token))); }
    @GetMapping("/preview")
    public Result<CartApplicationService.Preview> preview(@RequestHeader("Authorization") String token) { return Result.ok(service.preview(user(token))); }
    @GetMapping("/internal/items")
    public Result<List<CartItemDTO>> internal(@RequestHeader("X-User-Id") String userId) { return Result.ok(service.list(userId)); }
}
