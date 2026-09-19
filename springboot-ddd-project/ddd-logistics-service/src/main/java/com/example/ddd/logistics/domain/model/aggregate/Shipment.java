package com.example.ddd.logistics.domain.model.aggregate;

import com.example.ddd.common.domain.model.BaseAggregateRoot;
import com.example.ddd.contract.logistics.event.*;
import java.time.LocalDateTime;
import java.util.*;

/** 领域层发货聚合：包含不可变商品快照和追加式轨迹；签收仅一次，退款拦截后停止运输。 */
public class Shipment extends BaseAggregateRoot {
    /** 承运商值对象，无独立标识。 */
    public enum Carrier { SF, YTO, ZTO, MOCK }
    /** 物流生命周期，REJECTED 表示退款拦截。 */
    public enum Status { SHIPPED, IN_TRANSIT, DELIVERED, REJECTED }
    /** 包裹商品快照，数量必须正数。 */
    public record Item(String skuId,String name,int quantity) {
        public Item { if(quantity<=0) throw new IllegalArgumentException("发货数量必须大于零"); }
    }
    /** 轨迹实体：trackId 唯一标识一次物流扫描。 */
    public record TrackingRecord(String trackId,LocalDateTime time,String location,String description) {}
    /** 持久化重建用不可变快照。 */
    public record State(String shipmentId,String orderId,String orderNo,String userId,Carrier carrier,String trackingNo,
                        Status status,String receiver,String mobile,String address,LocalDateTime shippedAt,
                        LocalDateTime deliveredAt,List<Item> items,List<TrackingRecord> tracks) {
        public State { items=List.copyOf(items); tracks=List.copyOf(tracks); }
    }
    private State state;
    private Shipment(State state) { this.state=state; }
    public static Shipment restore(State state) { return new Shipment(state); }
    public static Shipment create(String id,String orderId,String orderNo,String userId,String receiver,String mobile,
                                  String address,List<Item> items,Carrier carrier) {
        if(items.isEmpty()) throw new IllegalArgumentException("发货明细不能为空");
        var now=LocalDateTime.now();
        var shipment=new Shipment(new State(id,orderId,orderNo,userId,carrier,carrier.name()+id,Status.SHIPPED,
                receiver,mobile,address,now,null,items,List.of(new TrackingRecord(UUID.randomUUID().toString(),now,"发货仓","已揽收"))));
        shipment.registerEvent(new ShipmentCreatedEvent(id,orderId,orderNo,userId,carrier.name(),shipment.state.trackingNo(),now));
        return shipment;
    }
    public void advance() {
        if(state.status()!=Status.SHIPPED) return;
        transition(Status.IN_TRANSIT,"转运中心","运输中",null);
    }
    public void deliver() {
        if(state.status()==Status.DELIVERED) return;
        if(state.status()==Status.REJECTED) throw new IllegalStateException("已拦截的包裹不能签收");
        var now=LocalDateTime.now(); transition(Status.DELIVERED,"收货地址","收件人已签收",now);
        registerEvent(new ShipmentDeliveredEvent(state.shipmentId(),state.orderId(),state.orderNo(),state.userId(),now,state.receiver()));
    }
    public void reject() {
        if(state.status()==Status.REJECTED) return;
        transition(Status.REJECTED,"售后中心","退款拦截/退货处理中",state.deliveredAt());
    }
    private void transition(Status status,String location,String description,LocalDateTime deliveredAt) {
        var tracks=new ArrayList<>(state.tracks());
        tracks.add(new TrackingRecord(UUID.randomUUID().toString(),LocalDateTime.now(),location,description));
        state=new State(state.shipmentId(),state.orderId(),state.orderNo(),state.userId(),state.carrier(),state.trackingNo(),
                status,state.receiver(),state.mobile(),state.address(),state.shippedAt(),deliveredAt,state.items(),tracks);
    }
    public State state() { return state; }
    @Override public String aggregateId() { return state.shipmentId(); }
}
