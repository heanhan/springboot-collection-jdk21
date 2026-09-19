package com.example.ddd.inventory.interfaces.rest;

import com.example.ddd.common.result.Result;
import com.example.ddd.inventory.application.command.CreateWarehouseCommand;
import com.example.ddd.inventory.application.service.WarehouseApplicationService;
import com.example.ddd.inventory.interfaces.assembler.InventoryAssembler;
import com.example.ddd.inventory.interfaces.dto.InventoryRequests.CreateWarehouseRequest;
import com.example.ddd.inventory.interfaces.dto.InventoryRequests.UpdateWarehouseRequest;
import com.example.ddd.inventory.interfaces.dto.InventoryResponses.WarehouseView;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST 控制器：仓库管理接口（面向运营后台）。
 */
@RestController
@RequestMapping("/inventory/warehouses")
public class WarehouseController {

    private final WarehouseApplicationService warehouseService;

    public WarehouseController(WarehouseApplicationService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @PostMapping
    public Result<String> create(@Valid @RequestBody CreateWarehouseRequest request) {
        String id = warehouseService.createWarehouse(new CreateWarehouseCommand(
                request.code(), request.name(), request.province(), request.city(),
                request.district(), request.address(), request.contact(), request.phone(),
                request.priority()));
        return Result.ok(id);
    }

    @PutMapping("/{warehouseId}")
    public Result<Void> update(@PathVariable("warehouseId") String warehouseId,
                               @RequestBody UpdateWarehouseRequest request) {
        warehouseService.updateWarehouse(warehouseId, request.name(), request.province(),
                request.city(), request.district(), request.address(), request.contact(),
                request.phone(), request.priority());
        if (request.enabled() != null) {
            warehouseService.changeStatus(warehouseId, request.enabled());
        }
        return Result.ok();
    }

    @GetMapping("/{warehouseId}")
    public Result<WarehouseView> get(@PathVariable("warehouseId") String warehouseId) {
        return Result.ok(InventoryAssembler.toView(warehouseService.getWarehouse(warehouseId)));
    }

    @GetMapping
    public Result<List<WarehouseView>> listEnabled() {
        List<WarehouseView> views = warehouseService.listEnabled().stream()
                .map(InventoryAssembler::toView).toList();
        return Result.ok(views);
    }
}
