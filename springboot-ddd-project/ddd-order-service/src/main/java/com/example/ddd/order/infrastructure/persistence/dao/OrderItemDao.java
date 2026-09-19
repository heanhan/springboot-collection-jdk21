package com.example.ddd.order.infrastructure.persistence.dao;

import com.example.ddd.order.infrastructure.persistence.po.OrderItemPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA DAO：t_order_item。
 */
public interface OrderItemDao extends JpaRepository<OrderItemPO, String> {

    @Query("SELECT i FROM OrderItemPO i WHERE i.orderId = :orderId AND i.deleted = 0")
    List<OrderItemPO> findByOrderId(@Param("orderId") String orderId);

    @Query("SELECT i FROM OrderItemPO i WHERE i.itemId = :itemId AND i.deleted = 0")
    java.util.Optional<OrderItemPO> findByIdAndNotDeleted(@Param("itemId") String itemId);
}
