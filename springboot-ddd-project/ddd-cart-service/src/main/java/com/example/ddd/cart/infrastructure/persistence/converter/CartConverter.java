package com.example.ddd.cart.infrastructure.persistence.converter;

import com.example.ddd.cart.domain.model.aggregate.Cart;
import com.example.ddd.cart.infrastructure.persistence.po.CartPO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.util.List;

/** 基础设施转换：隔离 JSON 存储格式和纯领域集合。 */
@Component
public class CartConverter {
    private final ObjectMapper json;
    public CartConverter(ObjectMapper json) { this.json=json; }
    public Cart domain(CartPO p) {
        try { return new Cart(p.userId,json.readValue(p.items,new TypeReference<List<Cart.Item>>() {})); }
        catch(Exception e) { throw new IllegalStateException("购物车快照损坏",e); }
    }
    public void copy(Cart cart,CartPO p) {
        p.userId=cart.aggregateId();
        try { p.items=json.writeValueAsString(cart.items()); }
        catch(Exception e) { throw new IllegalStateException("购物车序列化失败",e); }
    }
}
