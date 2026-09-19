package com.example.ddd.inventory.infrastructure.persistence.repository;

import com.example.ddd.inventory.domain.model.entity.StockTransaction;
import com.example.ddd.inventory.domain.model.valueobject.TransactionType;
import com.example.ddd.inventory.domain.repository.StockTransactionRepository;
import com.example.ddd.inventory.infrastructure.persistence.converter.InventoryConverter;
import com.example.ddd.inventory.infrastructure.persistence.dao.StockTransactionDao;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 仓储实现：StockTransaction 流水（追加式）。
 *
 * <p><b>只增不改：</b>{@link #append(StockTransaction)} 只做 insert，没有 update 方法，
 * 呼应流水"历史事实不可变"的领域语义。</p>
 */
@Repository
public class StockTransactionRepositoryImpl implements StockTransactionRepository {

    private final StockTransactionDao dao;

    public StockTransactionRepositoryImpl(StockTransactionDao dao) {
        this.dao = dao;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StockTransaction> findIdempotent(String bizNo, TransactionType type,
                                                     String warehouseId, String skuId) {
        return dao.findIdempotent(bizNo, type, warehouseId, skuId).map(InventoryConverter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockTransaction> findByBizNo(String bizNo) {
        return dao.findByBizNo(bizNo).stream().map(InventoryConverter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockTransaction> findByBizNoAndType(String bizNo, TransactionType type) {
        return dao.findByBizNoAndType(bizNo, type).stream().map(InventoryConverter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByBizNoAndType(String bizNo, TransactionType type) {
        return dao.existsByBizNoAndType(bizNo, type);
    }

    @Override
    @Transactional
    public void append(StockTransaction tx) {
        dao.save(InventoryConverter.toPO(tx));
    }
}
