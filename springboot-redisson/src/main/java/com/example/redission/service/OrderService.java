package com.example.redission.service;

import com.example.redission.entity.Order;

public interface OrderService {

    /**
     * 创建订单
     * @param order
     * @return
     */
    Order addOrderInfo(Order order);
}
