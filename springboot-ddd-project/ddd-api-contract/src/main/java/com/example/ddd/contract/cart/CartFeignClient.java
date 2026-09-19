package com.example.ddd.contract.cart;

import com.example.ddd.common.result.Result;
import com.example.ddd.contract.ServiceNames;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

/**
 * 购物车服务对外 Feign 契约。
 *
 * @author ddd-learning
 */
@FeignClient(name = ServiceNames.CART, contextId = "cartFeignClient", path = "/cart/internal", url = "${ddd.services.cart-url:http://localhost:8088}")
public interface CartFeignClient {

    /**
     * 查询用户购物车明细（下单时把选中的条目转成订单）。
     *
     * @param userId 用户 ID (从 Header 传递)
     */
    @GetMapping("/items")
    Result<List<CartItemDTO>> listItems(@RequestHeader("X-User-Id") String userId);
}
