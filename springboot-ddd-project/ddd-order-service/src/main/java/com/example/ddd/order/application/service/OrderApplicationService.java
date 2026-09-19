package com.example.ddd.order.application.service;

import com.example.ddd.common.domain.event.DomainEvent;
import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;
import com.example.ddd.common.util.IdGenerator;
import com.example.ddd.order.application.command.PlaceOrderCommand;
import com.example.ddd.order.application.port.DomainEventPublisher;
import com.example.ddd.order.application.port.InventoryGateway;
import com.example.ddd.order.application.port.ProductGateway;
import com.example.ddd.order.domain.model.aggregate.Order;
import com.example.ddd.order.domain.model.entity.OrderItem;
import com.example.ddd.order.domain.model.valueobject.OrderStatus;
import com.example.ddd.order.domain.repository.OrderRepository;
import com.example.ddd.order.domain.service.OrderPricingService;
import com.example.ddd.order.domain.service.OrderPricingService.PricingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 应用服务：订单用例编排。
 *
 * <p><b>用例：</b>下单、支付回调置为已付、取消、发货、确认收货、超时关单扫描、查询。</p>
 *
 * <p><b>下单编排（对应 plan 第七节链路 1）：</b>
 * <ol>
 *   <li>经 {@link ProductGateway} 防腐层校验商品在售并取价（快照）。</li>
 *   <li>{@link OrderPricingService} 计算总额 / 运费 / 优惠 / 实付。</li>
 *   <li>{@link Order#create} 构建聚合根（收集 OrderCreatedEvent）。</li>
 *   <li>经 {@link InventoryGateway} 预占库存；失败抛异常 → 事务回滚，订单不落库。</li>
 *   <li>持久化订单，事务提交后发布领域事件。</li>
 * </ol>
 *
 * <p><b>分布式一致性说明（学习项目取舍）：</b>"预占库存(远程) + 保存订单(本地)"是典型的双写问题。
 * 本实现选择"先预占后落库"，若落库失败会残留预占，需靠对账 / 补偿释放。
 * 生产环境应引入 Saga 或本地消息表(Outbox) + 事务消息保证最终一致。</p>
 *
 * <p><b>事务边界：</b>每个写方法 {@code @Transactional}；事件通过发布器在提交后投递。</p>
 */
@Service
public class OrderApplicationService {

    private static final Logger log = LoggerFactory.getLogger(OrderApplicationService.class);
    private static final DateTimeFormatter ORDER_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderRepository orderRepository;
    private final OrderPricingService pricingService;
    private final ProductGateway productGateway;
    private final InventoryGateway inventoryGateway;
    private final DomainEventPublisher eventPublisher;
    private final int paymentTimeoutMinutes;

    public OrderApplicationService(OrderRepository orderRepository,
                                   OrderPricingService pricingService,
                                   ProductGateway productGateway,
                                   InventoryGateway inventoryGateway,
                                   DomainEventPublisher eventPublisher,
                                   @Value("${ddd.order.payment-timeout-minutes:30}") int paymentTimeoutMinutes) {
        this.orderRepository = orderRepository;
        this.pricingService = pricingService;
        this.productGateway = productGateway;
        this.inventoryGateway = inventoryGateway;
        this.eventPublisher = eventPublisher;
        this.paymentTimeoutMinutes = paymentTimeoutMinutes;
    }

    // ============================================================
    // 用例 1：下单
    // ============================================================

    @Transactional
    public String placeOrder(PlaceOrderCommand cmd) {
        if (cmd.items() == null || cmd.items().isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_ITEM_EMPTY);
        }

        // 1. 校验商品 + 构建订单项快照
        if (cmd.items().stream().map(PlaceOrderCommand.Item::skuId).distinct().count() != cmd.items().size()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "同一 SKU 请合并数量后提交");
        }
        List<OrderItem> items = new ArrayList<>();
        List<InventoryGateway.LockItem> lockItems = new ArrayList<>();
        for (PlaceOrderCommand.Item ci : cmd.items()) {
            ProductGateway.SkuSnapshot sku = productGateway.getSku(ci.skuId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_SKU_NOT_FOUND,
                            "SKU 不存在: " + ci.skuId()));
            if (!sku.onSale()) {
                throw new BusinessException(ErrorCode.PRODUCT_STATUS_ILLEGAL,
                        "商品不可购买: " + sku.spuName());
            }
            items.add(new OrderItem(IdGenerator.nextIdStr(), sku.skuId(), sku.spuId(), sku.spuName(),
                    sku.skuName(), sku.image(), sku.price(), ci.quantity()));
            lockItems.add(new InventoryGateway.LockItem(sku.skuId(), ci.quantity()));
        }

        // 2. 定价
        PricingResult pricing = pricingService.price(items);

        // 3. 构建订单聚合根（收集 OrderCreatedEvent）
        String orderId = IdGenerator.nextIdStr();
        Order order = Order.create(orderId, generateOrderNo(), cmd.userId(), items, pricing,
                cmd.shippingAddress(), cmd.remark(), paymentTimeoutMinutes);

        // 4. 预占库存（失败则抛异常，事务回滚）
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK) {
                            try { inventoryGateway.release(orderId, "ORDER_ROLLBACK"); }
                            catch (Exception e) { log.error("库存补偿失败，需对账 orderId={}", orderId, e); }
                        }
                    }
                });
        inventoryGateway.lock(orderId, lockItems);

        // 5. 落库 + 发布事件
        orderRepository.save(order);
        publishEvents(order);
        log.info("[Order] placed orderId={} orderNo={} userId={} payAmount={}",
                orderId, order.getOrderNo(), cmd.userId(), order.getPayAmount());
        return orderId;
    }

    // ============================================================
    // 用例 2：支付成功（由 PaymentSuccess 事件消费触发）
    // ============================================================

    @Transactional
    public void markPaid(String orderId, String channel, String tradeNo, LocalDateTime paidAt) {
        Order order = loadOrder(orderId);
        if (order.getPaidAt() != null) {
            log.info("[Order] markPaid idempotent hit orderId={}", orderId);
            return;
        }
        if (order.getStatus() == OrderStatus.CANCELLED) return;
        order.pay(channel, tradeNo, paidAt);
        orderRepository.save(order);
        publishEvents(order);
        log.info("[Order] paid orderId={} channel={} tradeNo={}", orderId, channel, tradeNo);
    }

    // ============================================================
    // 用例 3：取消订单
    // ============================================================

    @Transactional
    public void cancel(String orderId, String userId, String reasonCode, String reasonDesc) {
        Order order = loadOrder(orderId);
        assertOwner(order, userId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.info("[Order] cancel idempotent hit orderId={}", orderId);
            return;
        }
        order.cancel(reasonCode, reasonDesc);
        orderRepository.save(order);
        // 释放预占库存（幂等：inventory 侧按 bizNo 去重）
        // 库存释放由已提交的取消事件驱动，避免本地回滚而远程已经释放。
        publishEvents(order);
        log.info("[Order] cancelled orderId={} reason={}", orderId, reasonCode);
    }

    // ============================================================
    // 用例 4：发货（由物流事件触发）
    // ============================================================

    @Transactional
    public void ship(String orderId, String shipmentId, String carrier, String trackingNo) {
        Order order = loadOrder(orderId);
        if (order.getShippedAt() != null) {
            log.info("[Order] ship idempotent hit orderId={}", orderId);
            return;
        }
        if (order.getStatus() == OrderStatus.REFUNDED) return;
        order.ship(shipmentId, carrier, trackingNo, LocalDateTime.now());
        orderRepository.save(order);
        publishEvents(order);
        log.info("[Order] shipped orderId={} trackingNo={}", orderId, trackingNo);
    }

    // ============================================================
    // 用例 5：确认收货
    // ============================================================

    @Transactional
    public void complete(String orderId, String userId) {
        Order order = loadOrder(orderId);
        assertOwner(order, userId);
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.REFUNDED) return;
        order.complete(LocalDateTime.now());
        orderRepository.save(order);
        publishEvents(order);
        log.info("[Order] completed orderId={}", orderId);
    }

    // ============================================================
    // 用例 6：超时关单扫描（由定时任务逐单调用 cancel）
    // ============================================================

    @Transactional(readOnly = true)
    public List<Order> findTimeoutOrders(int limit) {
        return orderRepository.findTimeoutUnpaid(LocalDateTime.now(), limit);
    }

    // ============================================================
    // 查询
    // ============================================================

    @Transactional(readOnly = true)
    public Order getOrder(String orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<Order> listByUser(String userId, OrderStatus status, int pageNum, int pageSize) {
        return orderRepository.findByUser(userId, status, pageNum, pageSize);
    }

    // ============================================================
    // 内部工具
    // ============================================================

    private Order loadOrder(String orderId) {
        return orderRepository.findForUpdate(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    /** 退款成功事件用例：同一事务推进退款状态，无重复副作用。 */
    @Transactional
    public void refunded(String orderId) {
        Order order = loadOrder(orderId);
        if (order.getStatus() == OrderStatus.REFUNDED || order.getStatus() == OrderStatus.CANCELLED) return;
        order.startRefund();
        order.finishRefund();
        orderRepository.save(order);
    }

    private void assertOwner(Order order, String userId) {
        if (userId != null && !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作他人订单");
        }
    }

    private void publishEvents(Order order) {
        for (DomainEvent event : order.getDomainEvents()) {
            eventPublisher.publish(event);
        }
        order.clearDomainEvents();
    }

    /** 生成用户可见订单号：时间戳 + 4 位随机。 */
    private String generateOrderNo() {
        return ORDER_NO_FMT.format(LocalDateTime.now()) + ThreadLocalRandom.current().nextInt(1000, 10000);
    }
}
