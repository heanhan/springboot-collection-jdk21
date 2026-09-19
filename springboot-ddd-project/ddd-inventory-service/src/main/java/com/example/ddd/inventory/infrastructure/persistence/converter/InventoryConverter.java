package com.example.ddd.inventory.infrastructure.persistence.converter;

import com.example.ddd.inventory.domain.model.aggregate.Stock;
import com.example.ddd.inventory.domain.model.aggregate.Warehouse;
import com.example.ddd.inventory.domain.model.entity.StockTransaction;
import com.example.ddd.inventory.infrastructure.persistence.po.StockPO;
import com.example.ddd.inventory.infrastructure.persistence.po.StockTransactionPO;
import com.example.ddd.inventory.infrastructure.persistence.po.WarehousePO;

/**
 * PO ⇄ Domain 转换器。
 *
 * <p><b>手写转换的原因：</b>学习项目更看重"看得懂每个字段的映射意图"，
 * 且需要在转换时做语义翻译（例如 status 字符串 ⇄ boolean enabled）。</p>
 *
 * <p><b>重建 (reconstitute)：</b>从 PO 还原领域对象时使用静态工厂 {@code reconstitute}，
 * 绕过创建时的业务校验（数据已入库，说明当初已通过校验），并回填乐观锁 version。</p>
 */
public final class InventoryConverter {

    private InventoryConverter() {}

    // ============================================================
    // Stock
    // ============================================================

    public static Stock toDomain(StockPO po) {
        return Stock.reconstitute(po.getStockId(), po.getWarehouseId(), po.getSkuId(),
                nvl(po.getAvailableQty()), nvl(po.getLockedQty()), nvl(po.getWarnQty()),
                po.getVersion() == null ? 0L : po.getVersion());
    }

    public static StockPO toPO(Stock stock) {
        StockPO po = new StockPO();
        po.setStockId(stock.getStockId());
        po.setWarehouseId(stock.getWarehouseId());
        po.setSkuId(stock.getSkuId());
        po.setAvailableQty(stock.getAvailableQty());
        po.setLockedQty(stock.getLockedQty());
        po.setWarnQty(stock.getWarnQty());
        return po;
    }

    // ============================================================
    // Warehouse
    // ============================================================

    public static Warehouse toDomain(WarehousePO po) {
        return Warehouse.reconstitute(po.getWarehouseId(), po.getCode(), po.getName(),
                po.getProvince(), po.getCity(), po.getDistrict(), po.getAddress(),
                po.getContact(), po.getPhone(), nvl(po.getPriority()),
                WarehousePO.STATUS_ACTIVE.equals(po.getStatus()));
    }

    public static WarehousePO toPO(Warehouse warehouse) {
        WarehousePO po = new WarehousePO();
        po.setWarehouseId(warehouse.getWarehouseId());
        po.setCode(warehouse.getCode());
        po.setName(warehouse.getName());
        po.setProvince(warehouse.getProvince());
        po.setCity(warehouse.getCity());
        po.setDistrict(warehouse.getDistrict());
        po.setAddress(warehouse.getAddress());
        po.setContact(warehouse.getContact());
        po.setPhone(warehouse.getPhone());
        po.setPriority(warehouse.getPriority());
        po.setStatus(warehouse.isEnabled() ? WarehousePO.STATUS_ACTIVE : WarehousePO.STATUS_DISABLED);
        return po;
    }

    // ============================================================
    // StockTransaction
    // ============================================================

    public static StockTransaction toDomain(StockTransactionPO po) {
        return new StockTransaction(po.getTransactionId(), po.getBizNo(), po.getType(),
                po.getWarehouseId(), po.getSkuId(), nvl(po.getQuantity()),
                nvl(po.getBeforeQty()), nvl(po.getAfterQty()),
                nvl(po.getBeforeLocked()), nvl(po.getAfterLocked()),
                po.getReason(), po.getCreateTime());
    }

    public static StockTransactionPO toPO(StockTransaction tx) {
        StockTransactionPO po = new StockTransactionPO();
        po.setTransactionId(tx.getTransactionId());
        po.setBizNo(tx.getBizNo());
        po.setType(tx.getType());
        po.setWarehouseId(tx.getWarehouseId());
        po.setSkuId(tx.getSkuId());
        po.setQuantity(tx.getQuantity());
        po.setBeforeQty(tx.getBeforeQty());
        po.setAfterQty(tx.getAfterQty());
        po.setBeforeLocked(tx.getBeforeLocked());
        po.setAfterLocked(tx.getAfterLocked());
        po.setReason(tx.getReason());
        po.setCreateTime(tx.getCreateTime());
        return po;
    }

    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }
}
