package com.example.ddd.cart.infrastructure.persistence.po;

import jakarta.persistence.*;

/** JPA 购物车快照：最多 100 条目，以用户为事务边界；版本字段不进入领域层。 */
@Entity
@Table(name="t_cart")
public class CartPO {
    @Id @Column(length=64) public String userId;
    @Column(columnDefinition="LONGTEXT") public String items;
    @Version public Long version;
}
