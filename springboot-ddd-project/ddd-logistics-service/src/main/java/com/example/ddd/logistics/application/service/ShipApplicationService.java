package com.example.ddd.logistics.application.service;

import com.example.ddd.common.exception.*;
import com.example.ddd.common.util.IdGenerator;
import com.example.ddd.logistics.application.port.FulfillmentPorts;
import com.example.ddd.logistics.domain.model.aggregate.Shipment;
import com.example.ddd.logistics.domain.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** 应用层履约编排：支付事件触发实扣和建单；本地事务保证发货单与 Outbox 原子写入。 */
@Service
public class ShipApplicationService {
    private final ShipmentRepository repository; private final FulfillmentPorts.Gateway gateway; private final FulfillmentPorts.Events events;
    public ShipApplicationService(ShipmentRepository repository,FulfillmentPorts.Gateway gateway,FulfillmentPorts.Events events) {
        this.repository=repository; this.gateway=gateway; this.events=events;
    }
    /** 发货用例：同订单唯一键兜底重复建单；库存实扣按 orderId 幂等，数据库失败可安全重试。 */
    @Transactional
    public void ship(String orderId) {
        if(repository.byOrder(orderId).isPresent()) return;
        var o=gateway.order(orderId);
        if(gateway.refunded(orderId) || "REFUNDED".equals(o.status())) return;
        if(!"PAID".equals(o.status())) throw new IllegalStateException("订单尚未到可发货状态");
        gateway.deduct(orderId);
        var shipment=Shipment.create(IdGenerator.nextIdStr(),o.orderId(),o.orderNo(),o.userId(),o.receiver(),o.receiverMobile(),
                o.shippingAddress(),o.items().stream().map(i -> new Shipment.Item(i.skuId(),i.spuName(),i.quantity())).toList(),Shipment.Carrier.MOCK);
        save(shipment);
    }
    /** 签收用例：验证归属并检查退款拦截；重复调用不新增轨迹和事件。 */
    @Transactional
    public void deliver(String id,String user) {
        var s=load(id,true);
        if(!s.state().userId().equals(user)) throw new BusinessException(ErrorCode.FORBIDDEN);
        if(gateway.refunded(s.state().orderId())) throw new BusinessException(ErrorCode.BAD_REQUEST,"退款包裹不能签收");
        s.deliver(); save(s);
    }
    /** 模拟承运商推进：每次一个独立事务，退款时终止运输；外部依赖失败则不推进。 */
    @Transactional
    public void advance(String id) {
        var s=load(id,true);
        if(gateway.refunded(s.state().orderId())) s.reject();
        else if(s.state().status()==Shipment.Status.SHIPPED) s.advance();
        else if(s.state().status()==Shipment.Status.IN_TRANSIT) s.deliver();
        save(s);
    }
    /** 查询待推进发货单的有限批次。 */
    @Transactional(readOnly=true)
    public List<String> activeIds() { return repository.activeIds(); }
    /** 查询完整物流轨迹。 */
    @Transactional(readOnly=true)
    public Shipment.State get(String id) { return load(id,false).state(); }
    /** 查询订单对应发货单；当前一个订单一个包裹，契约保留多包裹扩展能力。 */
    @Transactional(readOnly=true)
    public List<Shipment.State> byOrder(String id) { return repository.byOrder(id).stream().map(Shipment::state).toList(); }
    private Shipment load(String id,boolean lock) { return repository.find(id,lock).orElseThrow(() -> new BusinessException(ErrorCode.LOGISTICS_SHIPMENT_NOT_FOUND)); }
    private void save(Shipment s) { repository.save(s); s.getDomainEvents().forEach(events::publish); }
}
