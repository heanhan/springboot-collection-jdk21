package com.example.ddd.logistics.infrastructure.persistence.converter;

import com.example.ddd.logistics.domain.model.aggregate.Shipment;
import com.example.ddd.logistics.infrastructure.persistence.po.ShipmentPO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.util.List;

/** 基础设施转换：JSON 仅存在于此处，领域层始终使用强类型、不可变集合。 */
@Component
public class ShipmentConverter {
    private final ObjectMapper json;
    public ShipmentConverter(ObjectMapper json) { this.json=json; }
    public Shipment domain(ShipmentPO p) {
        try {
            return Shipment.restore(new Shipment.State(p.shipmentId,p.orderId,p.orderNo,p.userId,Shipment.Carrier.valueOf(p.carrier),
                    p.trackingNo,Shipment.Status.valueOf(p.status),p.receiver,p.mobile,p.address,p.shippedAt,p.deliveredAt,
                    json.readValue(p.items,new TypeReference<List<Shipment.Item>>() {}),
                    json.readValue(p.tracks,new TypeReference<List<Shipment.TrackingRecord>>() {})));
        } catch(Exception e) { throw new IllegalStateException("发货单快照损坏",e); }
    }
    public void copy(Shipment shipment,ShipmentPO p) {
        var s=shipment.state();
        p.shipmentId=s.shipmentId(); p.orderId=s.orderId(); p.orderNo=s.orderNo(); p.userId=s.userId();
        p.carrier=s.carrier().name(); p.trackingNo=s.trackingNo(); p.status=s.status().name(); p.receiver=s.receiver();
        p.mobile=s.mobile(); p.address=s.address(); p.shippedAt=s.shippedAt(); p.deliveredAt=s.deliveredAt();
        try { p.items=json.writeValueAsString(s.items()); p.tracks=json.writeValueAsString(s.tracks()); }
        catch(Exception e) { throw new IllegalStateException("发货单快照序列化失败",e); }
    }
}
