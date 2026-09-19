package com.example.ddd.logistics.domain.repository;

import com.example.ddd.logistics.domain.model.aggregate.Shipment;
import java.util.*;

/** 领域仓储端口：发货单与轨迹整体加载；写用例按聚合标识串行化。 */
public interface ShipmentRepository {
    Optional<Shipment> find(String id,boolean lock);
    Optional<Shipment> byOrder(String orderId);
    List<String> activeIds();
    void save(Shipment shipment);
}
