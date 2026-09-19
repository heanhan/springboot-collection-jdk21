package com.example.ddd.cart.infrastructure.persistence.repository;

import com.example.ddd.cart.domain.model.aggregate.Cart;
import com.example.ddd.cart.domain.repository.CartRepository;
import com.example.ddd.cart.infrastructure.persistence.po.CartPO;
import com.example.ddd.cart.infrastructure.persistence.converter.CartConverter;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import java.util.List;

/** JPA 仓储：MySQL 原子初始化购物车行再加锁，避免第一次并发加购丢失条目。 */
@Repository
public class CartRepositoryImpl implements CartRepository {
    @PersistenceContext private EntityManager em;
    private final CartConverter converter;
    public CartRepositoryImpl(CartConverter converter) { this.converter=converter; }
    public Cart load(String userId,boolean lock) {
        if(lock) em.createNativeQuery("INSERT INTO t_cart(user_id,items,version) VALUES (:id,'[]',0) ON DUPLICATE KEY UPDATE user_id=user_id")
                .setParameter("id",userId).executeUpdate();
        var p=lock?em.find(CartPO.class,userId,LockModeType.PESSIMISTIC_WRITE):em.find(CartPO.class,userId);
        return p==null?new Cart(userId,List.of()):converter.domain(p);
    }
    public void save(Cart cart) { converter.copy(cart,em.find(CartPO.class,cart.aggregateId())); }
}
