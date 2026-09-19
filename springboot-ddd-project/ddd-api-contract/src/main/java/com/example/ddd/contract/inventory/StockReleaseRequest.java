package com.example.ddd.contract.inventory;

import java.io.Serializable;

/**
 * 库存释放请求：把预占的库存归还给可用库存。
 *
 * @param bizNo  业务号（幂等键）
 * @param reason 释放原因，用于库存流水记录，例如 "ORDER_CANCELLED" / "ORDER_TIMEOUT"
 */
public record StockReleaseRequest(String bizNo, String reason) implements Serializable {
}
