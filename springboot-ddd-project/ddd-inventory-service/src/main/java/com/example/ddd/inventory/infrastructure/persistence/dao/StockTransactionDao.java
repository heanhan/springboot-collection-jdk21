package com.example.ddd.inventory.infrastructure.persistence.dao;

import com.example.ddd.inventory.domain.model.valueobject.TransactionType;
import com.example.ddd.inventory.infrastructure.persistence.po.StockTransactionPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA DAO：t_stock_transaction。
 */
public interface StockTransactionDao extends JpaRepository<StockTransactionPO, String> {

    @Query("SELECT t FROM StockTransactionPO t WHERE t.bizNo = :bizNo AND t.type = :type "
            + "AND t.warehouseId = :warehouseId AND t.skuId = :skuId")
    Optional<StockTransactionPO> findIdempotent(@Param("bizNo") String bizNo,
                                                @Param("type") TransactionType type,
                                                @Param("warehouseId") String warehouseId,
                                                @Param("skuId") String skuId);

    @Query("SELECT t FROM StockTransactionPO t WHERE t.bizNo = :bizNo ORDER BY t.createTime ASC")
    List<StockTransactionPO> findByBizNo(@Param("bizNo") String bizNo);

    @Query("SELECT t FROM StockTransactionPO t WHERE t.bizNo = :bizNo AND t.type = :type ORDER BY t.createTime ASC")
    List<StockTransactionPO> findByBizNoAndType(@Param("bizNo") String bizNo, @Param("type") TransactionType type);

    @Query("SELECT COUNT(t) > 0 FROM StockTransactionPO t WHERE t.bizNo = :bizNo AND t.type = :type")
    boolean existsByBizNoAndType(@Param("bizNo") String bizNo, @Param("type") TransactionType type);
}
