package com.example.ddd.cart.domain.model.aggregate;

import com.example.ddd.common.domain.model.BaseAggregateRoot;
import com.example.ddd.common.exception.*;
import java.util.*;

/** 领域层购物车聚合：用户是聚合标识，最多 100 个 SKU；加购不锁库存、不承诺价格。 */
public class Cart extends BaseAggregateRoot {
    /** 购物车条目实体，SKU 在当前购物车内唯一。 */
    public record Item(String skuId,int quantity,boolean checked) {
        public Item {
            if(skuId==null || skuId.isBlank() || quantity<1 || quantity>999) throw new BusinessException(ErrorCode.BAD_REQUEST,"购物车数量须为 1～999");
        }
    }
    private final String userId;
    private final Map<String,Item> items=new LinkedHashMap<>();
    public Cart(String userId,List<Item> values) {
        this.userId=Objects.requireNonNull(userId); values.forEach(i -> items.put(i.skuId(),i));
    }
    public void add(String skuId,int quantity) {
        new Item(skuId,quantity,true);
        var existing=items.get(skuId);
        if(existing==null && items.size()>=100) throw new BusinessException(ErrorCode.CART_ITEM_LIMIT_EXCEEDED);
        items.put(skuId,new Item(skuId,existing==null?quantity:Math.addExact(existing.quantity(),quantity),true));
    }
    public void change(String skuId,int quantity,boolean checked) {
        if(!items.containsKey(skuId)) throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        items.put(skuId,new Item(skuId,quantity,checked));
    }
    public void remove(String skuId) { items.remove(skuId); }
    public void clear() { items.clear(); }
    public List<Item> items() { return List.copyOf(items.values()); }
    @Override public String aggregateId() { return userId; }
}
