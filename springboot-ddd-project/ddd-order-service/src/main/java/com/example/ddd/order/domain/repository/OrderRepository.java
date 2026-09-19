package com.example.ddd.order.domain.repository;

import com.example.ddd.order.domain.model.aggregate.Order;
import com.example.ddd.order.domain.model.valueobject.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 仓储接口：Order 聚合（依赖倒置，实现在 infrastructure 层）。
 */
public interface OrderRepository {

    /** 写用例读取，串行化同一订单的支付、取消和履约状态转换。 */
    Optional<Order> findForUpdate(String orderId);

    Optional<Order> findById(String orderId);

    Optional<Order> findByOrderNo(String orderNo);

    /** 按用户 + 状态分页查询（状态为 null 表示全部）。 */
    List<Order> findByUser(String userId, OrderStatus status, int pageNum, int pageSize);

    /**
     * 查询已超时但未支付的订单（供定时关单任务扫描）。
     *
     * @param now   当前时间
     * @param limit 单次扫描上限
     */
    List<Order> findTimeoutUnpaid(LocalDateTime now, int limit);

    void save(Order order);
}
