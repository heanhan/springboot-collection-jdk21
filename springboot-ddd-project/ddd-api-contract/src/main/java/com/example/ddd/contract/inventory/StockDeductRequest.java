package com.example.ddd.contract.inventory;

import java.io.Serializable;

/**
 * 库存实扣请求：把预占的库存变成真正售出。
 *
 * @param bizNo 业务号（幂等键），传原下单时的 orderId
 */
public record StockDeductRequest(String bizNo) implements Serializable {
}
