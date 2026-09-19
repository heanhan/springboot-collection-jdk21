package com.example.ddd.order.infrastructure.persistence.dao;

import com.example.ddd.order.infrastructure.persistence.po.OrderPO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA DAO：t_order。
 */
public interface OrderDao extends JpaRepository<OrderPO, String> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrderPO o WHERE o.orderId = :id AND o.deleted = 0")
    Optional<OrderPO> findForUpdate(@Param("id") String id);

    @Query("SELECT o FROM OrderPO o WHERE o.orderId = :orderId AND o.deleted = 0")
    Optional<OrderPO> findByIdAndNotDeleted(@Param("orderId") String orderId);

    @Query("SELECT o FROM OrderPO o WHERE o.orderNo = :orderNo AND o.deleted = 0")
    Optional<OrderPO> findByOrderNo(@Param("orderNo") String orderNo);

    @Query("SELECT o FROM OrderPO o WHERE o.deleted = 0 AND o.userId = :userId "
            + "AND (:status IS NULL OR o.status = :status) "
            + "ORDER BY o.createdAt DESC")
    List<OrderPO> findByUser(@Param("userId") String userId, @Param("status") String status, Pageable pageable);

    /** 超时未支付扫描：status = CREATED 且 expire_at < now。 */
    @Query("SELECT o FROM OrderPO o WHERE o.deleted = 0 AND o.status = 'CREATED' AND o.expireAt < :now "
            + "ORDER BY o.expireAt ASC")
    List<OrderPO> findTimeoutUnpaid(@Param("now") LocalDateTime now, Pageable pageable);
}
