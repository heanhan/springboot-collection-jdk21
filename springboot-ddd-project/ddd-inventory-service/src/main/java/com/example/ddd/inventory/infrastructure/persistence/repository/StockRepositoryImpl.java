package com.example.ddd.inventory.infrastructure.persistence.repository;

import com.example.ddd.inventory.domain.model.aggregate.Stock;
import com.example.ddd.inventory.domain.repository.StockRepository;
import com.example.ddd.inventory.infrastructure.persistence.converter.InventoryConverter;
import com.example.ddd.inventory.infrastructure.persistence.dao.StockDao;
import com.example.ddd.inventory.infrastructure.persistence.po.StockPO;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 仓储实现：Stock 聚合。
 *
 * <p><b>乐观锁保存策略：</b>{@link #save(Stock)} 会先按主键取回受管 PO，
 * 回填 {@code createTime} 与 {@code version} 后再 save。这样 JPA 的 {@code @Version} 会在
 * UPDATE 语句加上 {@code WHERE version = ?}：若期间被其他事务修改，version 不匹配则更新 0 行，
 * 抛 {@link org.springframework.orm.ObjectOptimisticLockingFailureException}，由应用层重试。</p>
 */
@Repository
public class StockRepositoryImpl implements StockRepository {

    private final StockDao stockDao;

    public StockRepositoryImpl(StockDao stockDao) {
        this.stockDao = stockDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Stock> findById(String stockId) {
        return stockDao.findByIdAndNotDeleted(stockId).map(InventoryConverter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Stock> findByWarehouseAndSku(String warehouseId, String skuId) {
        return stockDao.findByWarehouseAndSku(warehouseId, skuId).map(InventoryConverter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Stock> findBySku(String skuId) {
        return stockDao.findBySkuId(skuId).stream().map(InventoryConverter::toDomain).toList();
    }

    @Override
    @Transactional
    public void save(Stock stock) {
        StockPO po = InventoryConverter.toPO(stock);
        stockDao.findByIdAndNotDeleted(stock.getStockId()).ifPresent(existing -> {
            po.setCreateTime(existing.getCreateTime());
            po.setVersion(existing.getVersion());
        });
        stockDao.save(po);
        // 回填自增后的 version 到领域对象，便于同一事务内后续判断
        stock.setVersion(po.getVersion() == null ? stock.getVersion() : po.getVersion());
    }
}
