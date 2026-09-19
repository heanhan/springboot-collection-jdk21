package com.example.ddd.contract.inventory;

import java.io.Serializable;
import java.util.List;

/**
 * 库存操作结果 DTO。
 *
 * @param bizNo       业务号
 * @param success     是否全部成功
 * @param failures    失败明细（例如某个 SKU 库存不足）
 * @param transactionId 库存服务生成的流水 ID，供后续实扣 / 释放引用
 */
public record StockOperationResultDTO(String bizNo,
                                      boolean success,
                                      String transactionId,
                                      List<Failure> failures) implements Serializable {

    /**
     * 失败明细。
     *
     * @param skuId       失败的 SKU
     * @param warehouseId 仓库 ID
     * @param requested   请求数量
     * @param available   实际可用数量
     * @param reason      失败原因
     */
    public record Failure(String skuId,
                          String warehouseId,
                          int requested,
                          int available,
                          String reason) implements Serializable {
    }
}
