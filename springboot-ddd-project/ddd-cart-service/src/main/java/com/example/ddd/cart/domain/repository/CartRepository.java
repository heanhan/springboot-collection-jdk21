package com.example.ddd.cart.domain.repository;

import com.example.ddd.cart.domain.model.aggregate.Cart;

/** 领域仓储端口：用户购物车整体读写，写入必须串行化同用户操作。 */
public interface CartRepository {
    Cart load(String userId,boolean lock);
    void save(Cart cart);
}
