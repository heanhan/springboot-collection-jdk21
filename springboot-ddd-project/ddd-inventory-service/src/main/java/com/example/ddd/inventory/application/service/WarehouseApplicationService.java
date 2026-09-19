package com.example.ddd.inventory.application.service;

import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;
import com.example.ddd.common.util.IdGenerator;
import com.example.ddd.inventory.application.command.CreateWarehouseCommand;
import com.example.ddd.inventory.domain.model.aggregate.Warehouse;
import com.example.ddd.inventory.domain.repository.WarehouseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 应用服务：仓库管理用例（创建 / 更新 / 启停 / 查询）。
 *
 * <p><b>事务边界：</b>每个写方法 {@code @Transactional}。仓库元数据变更频率低，
 * 无需像 {@link StockApplicationService} 那样做乐观锁重试。</p>
 */
@Service
public class WarehouseApplicationService {

    private static final Logger log = LoggerFactory.getLogger(WarehouseApplicationService.class);

    private final WarehouseRepository warehouseRepository;

    public WarehouseApplicationService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    @Transactional
    public String createWarehouse(CreateWarehouseCommand cmd) {
        if (warehouseRepository.existsByCode(cmd.code())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仓库编码已存在: " + cmd.code());
        }
        Warehouse warehouse = Warehouse.create(IdGenerator.nextIdStr(), cmd.code(), cmd.name(),
                cmd.province(), cmd.city(), cmd.district(), cmd.address(),
                cmd.contact(), cmd.phone(), cmd.priority());
        warehouseRepository.save(warehouse);
        log.info("[Inventory] warehouse created id={} code={}", warehouse.getWarehouseId(), cmd.code());
        return warehouse.getWarehouseId();
    }

    @Transactional
    public void updateWarehouse(String warehouseId, String name, String province, String city,
                                String district, String address, String contact, String phone, Integer priority) {
        Warehouse warehouse = load(warehouseId);
        warehouse.updateInfo(name, province, city, district, address, contact, phone, priority);
        warehouseRepository.save(warehouse);
    }

    @Transactional
    public void changeStatus(String warehouseId, boolean enabled) {
        Warehouse warehouse = load(warehouseId);
        if (enabled) {
            warehouse.enable();
        } else {
            warehouse.disable();
        }
        warehouseRepository.save(warehouse);
    }

    @Transactional(readOnly = true)
    public Warehouse getWarehouse(String warehouseId) {
        return load(warehouseId);
    }

    @Transactional(readOnly = true)
    public List<Warehouse> listEnabled() {
        return warehouseRepository.findAllEnabled();
    }

    private Warehouse load(String warehouseId) {
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_WAREHOUSE_NOT_FOUND));
    }
}
