package com.example.ddd.payment.application.service;

import com.example.ddd.common.exception.*;
import com.example.ddd.common.util.IdGenerator;
import com.example.ddd.contract.order.event.OrderCreatedEvent;
import com.example.ddd.payment.application.port.PaymentPorts;
import com.example.ddd.payment.domain.model.aggregate.*;
import com.example.ddd.payment.domain.repository.PaymentRepository;
import com.example.ddd.payment.domain.service.PaymentChannelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 应用层：支付建单、模拟回调、关闭与全额退款；每个写用例为一个本地事务。 */
@Service
public class PayApplicationService {
    private final PaymentRepository repository;
    private final PaymentPorts.Orders orders;
    private final PaymentPorts.Events events;
    private final PaymentChannelService channels;
    public PayApplicationService(PaymentRepository repository, PaymentPorts.Orders orders,
            PaymentPorts.Events events, PaymentChannelService channels) {
        this.repository=repository; this.orders=orders; this.events=events; this.channels=channels;
    }
    /** 订单创建消费：唯一订单约束兜底并发，已取消订单只创建关闭的支付单。 */
    @Transactional
    public void create(OrderCreatedEvent event) {
        if (repository.findByOrder(event.aggregateId()).isPresent()) return;
        var order=orders.get(event.aggregateId());
        var payment=PaymentOrder.create(IdGenerator.nextIdStr(),order.orderId(),order.orderNo(),order.userId(),order.payAmount(),event.getExpireAt());
        if ("CANCELLED".equals(order.status())) payment.close();
        repository.save(payment);
    }
    /** 选择模拟渠道：验证归属与订单仍可支付，返回模拟 URL。 */
    @Transactional
    public String pay(String id, String user, PaymentOrder.Channel channel) {
        var payment=load(id,true); owner(payment,user);
        if (!"CREATED".equals(orders.get(payment.state().orderId()).status())) throw new BusinessException(ErrorCode.BAD_REQUEST,"订单不可支付");
        payment.selectChannel(channel); repository.save(payment); return channels.payUrl(payment);
    }
    /** 模拟回调：锁定支付单、校验金额与流水、持久化成功事件；重复回调不重复发事件。 */
    @Transactional
    public void callback(String id,String user,BigDecimal amount,String tradeNo) {
        var payment=load(id,true); owner(payment,user);
        if (payment.state().status()==PaymentOrder.Status.PENDING && !"CREATED".equals(orders.get(payment.state().orderId()).status()))
            throw new BusinessException(ErrorCode.BAD_REQUEST,"订单不可支付");
        payment.confirm(amount,tradeNo,LocalDateTime.now()); repository.save(payment);
        payment.getDomainEvents().forEach(events::publish);
    }
    /** 订单取消消费：待支付则关单；已收款则全额补偿退款，处理支付与取消并发。 */
    @Transactional
    public void close(String orderId) {
        var candidate=repository.findByOrder(orderId);
        if (candidate.isEmpty()) throw new IllegalStateException("支付单尚未创建，稍后重试关单");
        var payment=load(candidate.get().aggregateId(),true);
        if (payment.state().status()==PaymentOrder.Status.SUCCESS) refundLocked(payment,"订单取消补偿退款");
        else if (payment.state().status()==PaymentOrder.Status.PENDING) { payment.close(); repository.save(payment); }
    }
    /** 用户全额退款：持有支付行锁，渠道成功后保存退款单和退款事件。 */
    @Transactional
    public RefundOrder.State refund(String id,String user,String reason) {
        var payment=load(id,true); owner(payment,user);
        return refundLocked(payment,reason).state();
    }
    private RefundOrder refundLocked(PaymentOrder payment,String reason) {
        var existing=repository.findRefund(payment.aggregateId());
        if (existing.isPresent()) return existing.get();
        payment.refund();
        var refund=RefundOrder.create(IdGenerator.nextIdStr(),payment,reason);
        channels.refund(payment,refund); refund.succeed();
        repository.save(payment); repository.save(refund); refund.getDomainEvents().forEach(events::publish);
        return refund;
    }
    /** 查询用例：只读事务，不修改领域状态。 */
    @Transactional(readOnly=true)
    public PaymentOrder.State get(String id) { return load(id,false).state(); }
    /** 按订单查询：供用户轮询异步支付建单。 */
    @Transactional(readOnly=true)
    public PaymentOrder.State byOrder(String id) {
        return repository.findByOrder(id).orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST,"支付单尚未创建")).state();
    }
    private PaymentOrder load(String id,boolean lock) {
        return repository.find(id,lock).orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST,"支付单不存在"));
    }
    private void owner(PaymentOrder payment,String user) {
        if (!payment.state().userId().equals(user)) throw new BusinessException(ErrorCode.FORBIDDEN);
    }
}
