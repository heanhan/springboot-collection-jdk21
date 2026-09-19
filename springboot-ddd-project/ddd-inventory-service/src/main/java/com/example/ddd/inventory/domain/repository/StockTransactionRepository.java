package com.example.ddd.inventory.domain.repository;

import com.example.ddd.inventory.domain.model.entity.StockTransaction;
import com.example.ddd.inventory.domain.model.valueobject.TransactionType;

import java.util.List;
import java.util.Optional;

/**
 * 仓储接口：StockTransaction 流水。
 *
 * <p><b>幂等设计：</b>库存所有写操作都以 {@code bizNo + type + warehouseId + skuId} 为幂等键，
 * 对应表的唯一索引 {@code uk_bizno_type_sku}。应用层在执行前先查重，避免 MQ 重投 / 用户重复点击导致重复扣减。</p>
 */
public interface StockTransactionRepository {

    /**
     * 幂等检查：某个 bizNo + type + skuId + warehouseId 是否已存在流水。
     * <p>存在则表示已经处理过，重复调用应直接返回。</p>
     */
    Optional<StockTransaction> findIdempotent(String bizNo, TransactionType type,
                                              String warehouseId, String skuId);

    /** 查询某业务号下的全部流水（对账 / 审计）。 */
    List<StockTransaction> findByBizNo(String bizNo);

    /**
     * 查询某业务号 + 某类型的全部流水。
     * <p>deduct / release 依赖它找到当初 LOCK 的明细，逐条回滚或实扣。</p>
     */
    List<StockTransaction> findByBizNoAndType(String bizNo, TransactionType type);

    /**
     * 命令级幂等：某业务号是否已存在某类型流水。
     * <p>用于 lock / deduct / release 的入口快速判定"是否已处理过"，命中则直接返回成功。</p>
     */
    boolean existsByBizNoAndType(String bizNo, TransactionType type);

    /** 追加一条流水（append-only，永不修改）。 */
    void append(StockTransaction tx);
}
