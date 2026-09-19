package com.example.ddd.logistics.infrastructure.persistence.repository;

import com.example.ddd.logistics.domain.model.aggregate.Shipment;
import com.example.ddd.logistics.domain.repository.ShipmentRepository;
import com.example.ddd.logistics.infrastructure.persistence.po.ShipmentPO;
import com.example.ddd.logistics.infrastructure.persistence.converter.ShipmentConverter;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import java.util.*;

/** JPA 仓储：一行装配完整聚合，业务写入持有数据库行锁，版本字段用于检测意外并发。 */
@Repository
public class ShipmentRepositoryImpl implements ShipmentRepository {
    @PersistenceContext private EntityManager em;
    private final ShipmentConverter converter;
    public ShipmentRepositoryImpl(ShipmentConverter converter) { this.converter=converter; }
    public Optional<Shipment> find(String id,boolean lock) {
        return Optional.ofNullable(lock?em.find(ShipmentPO.class,id,LockModeType.PESSIMISTIC_WRITE):em.find(ShipmentPO.class,id)).map(converter::domain);
    }
    public Optional<Shipment> byOrder(String orderId) {
        return em.createQuery("from ShipmentPO s where s.orderId=:id",ShipmentPO.class).setParameter("id",orderId)
                .getResultStream().findFirst().map(converter::domain);
    }
    public List<String> activeIds() {
        return em.createQuery("select s.shipmentId from ShipmentPO s where s.status in ('SHIPPED','IN_TRANSIT') order by s.shippedAt",String.class)
                .setMaxResults(100).getResultList();
    }
    public void save(Shipment shipment) {
        var p=em.find(ShipmentPO.class,shipment.aggregateId());
        if(p==null) { p=new ShipmentPO(); converter.copy(shipment,p); em.persist(p); }
        else converter.copy(shipment,p);
    }
}
