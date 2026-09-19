package com.example.ddd.inventory;

import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.inventory.domain.model.aggregate.*;
import com.example.ddd.inventory.domain.repository.*;
import com.example.ddd.inventory.domain.service.StockReservationService;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 库存核心规则回归：可用/预占守恒、跨仓优先级、全仓不足。 */
class InventoryTest {
    @Test void lockReleaseDeduct() {
        var stock=Stock.create("id","warehouse","sku",10,1);
        stock.lock(4); assertThat(stock.getAvailableQty()).isEqualTo(6); assertThat(stock.getLockedQty()).isEqualTo(4);
        stock.unlock(1); stock.deduct(3);
        assertThat(stock.getAvailableQty()).isEqualTo(7); assertThat(stock.getLockedQty()).isZero();
        assertThatThrownBy(() -> stock.lock(8)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> stock.deduct(1)).isInstanceOf(BusinessException.class);
    }
    @Test void splitsByWarehousePriorityAndFailsOnShortage() {
        var stocks=mock(StockRepository.class); var warehouses=mock(WarehouseRepository.class);
        var first=Warehouse.create("w1","A","A",null,null,null,null,null,null,1);
        var second=Warehouse.create("w2","B","B",null,null,null,null,null,null,2);
        when(warehouses.findAllEnabled()).thenReturn(List.of(second,first));
        when(stocks.findByWarehouseAndSku("w1","sku")).thenReturn(Optional.of(Stock.create("s1","w1","sku",3,0)));
        when(stocks.findByWarehouseAndSku("w2","sku")).thenReturn(Optional.of(Stock.create("s2","w2","sku",5,0)));
        var service=new StockReservationService(stocks,warehouses);
        assertThat(service.allocate("sku",6,null)).containsExactly(new StockReservationService.Allocation("w1","sku",3),new StockReservationService.Allocation("w2","sku",3));
        assertThatThrownBy(() -> service.allocate("sku",9,null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.allocate("sku",0,null)).isInstanceOf(BusinessException.class);
    }
}
