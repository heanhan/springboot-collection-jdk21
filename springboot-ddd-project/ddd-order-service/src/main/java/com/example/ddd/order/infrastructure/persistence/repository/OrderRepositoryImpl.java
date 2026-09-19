package com.example.ddd.order.infrastructure.persistence.repository;

import com.example.ddd.order.domain.model.aggregate.Order;
import com.example.ddd.order.domain.model.entity.OrderItem;
import com.example.ddd.order.domain.model.valueobject.OrderStatus;
import com.example.ddd.order.domain.repository.OrderRepository;
import com.example.ddd.order.infrastructure.persistence.converter.OrderConverter;
import com.example.ddd.order.infrastructure.persistence.dao.OrderDao;
import com.example.ddd.order.infrastructure.persistence.dao.OrderItemDao;
import com.example.ddd.order.infrastructure.persistence.po.OrderItemPO;
import com.example.ddd.order.infrastructure.persistence.po.OrderPO;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 仓储实现：Order 聚合。
 *
 * <p><b>聚合装配：</b>Order 与其全部 OrderItem 一起加载，保证聚合完整性
 * （订单项数量有限，一次性加载无性能问题）。</p>
 *
 * <p><b>保存策略：</b>OrderPO 按主键 upsert，回填 createTime / version 以配合乐观锁；
 * OrderItem 全量覆盖式保存（订单项创建后不可变，重复保存等价于无变化）。</p>
 */
@Repository
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderDao orderDao;
    private final OrderItemDao orderItemDao;

    public OrderRepositoryImpl(OrderDao orderDao, OrderItemDao orderItemDao) {
        this.orderDao = orderDao;
        this.orderItemDao = orderItemDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(String orderId) {
        return orderDao.findByIdAndNotDeleted(orderId).map(this::loadWithItems);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findByOrderNo(String orderNo) {
        return orderDao.findByOrderNo(orderNo).map(this::loadWithItems);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findByUser(String userId, OrderStatus status, int pageNum, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(0, pageNum - 1), pageSize);
        String statusCode = status == null ? null : status.name();
        return orderDao.findByUser(userId, statusCode, pageable).stream().map(this::loadWithItems).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findTimeoutUnpaid(LocalDateTime now, int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(1, limit));
        return orderDao.findTimeoutUnpaid(now, pageable).stream().map(this::loadWithItems).toList();
    }

    @Override
    @Transactional
    public void save(Order order) {
        OrderPO po = OrderConverter.toPO(order);
        orderDao.findByIdAndNotDeleted(order.getOrderId()).ifPresent(existing -> {
            po.setCreateTime(existing.getCreateTime());
            po.setVersion(existing.getVersion());
        });
        orderDao.save(po);

        for (OrderItem item : order.getItems()) {
            OrderItemPO itemPO = OrderConverter.toPO(item, order.getOrderId());
            orderItemDao.findByIdAndNotDeleted(item.getItemId()).ifPresent(existing -> {
                itemPO.setCreateTime(existing.getCreateTime());
                itemPO.setVersion(existing.getVersion());
            });
            orderItemDao.save(itemPO);
        }
    }

    @Override
    @Transactional
    public Optional<Order> findForUpdate(String orderId) {
        return orderDao.findForUpdate(orderId).map(this::loadWithItems);
    }

    private Order loadWithItems(OrderPO po) {
        List<OrderItemPO> itemPOs = orderItemDao.findByOrderId(po.getOrderId());
        return OrderConverter.toDomain(po, itemPOs);
    }
}
