package com.example.redission.service.impl;

import com.example.redission.dao.OrderDao;
import com.example.redission.entity.Order;
import com.example.redission.service.OrderService;
import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Resource
    private OrderDao orderDao;
    /**
     * 创建订单
     *
     * @param order
     * @return
     */
    @Transactional
    @Override
    public Order addOrderInfo(Order order) {
        return orderDao.save(order);
    }
}
