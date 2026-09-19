package com.example.ddd.inventory.application.command;

import java.util.List;

/**
 * 应用层结果对象：一次库存操作（lock / deduct / release）的结果。
 *
 * <p><b>为什么不直接返回契约 DTO？</b>
 * 应用层不应依赖对外契约（{@code ddd-api-contract}）的 DTO，否则内外耦合。
 * 由 interfaces 层的 assembler 把本对象转换成 {@code StockOperationResultDTO} 再返回。</p>
 *
 * @param bizNo         业务号
 * @param success       是否成功
 * @param transactionId 关联的库存流水 ID（多条时取首条），供对账使用
 * @param idempotent    本次是否为幂等重放命中（true 表示之前已处理，本次未再变更库存）
 * @param failures      失败明细
 */
public record StockOperationResult(String bizNo,
                                   boolean success,
                                   String transactionId,
                                   boolean idempotent,
                                   List<Failure> failures) {

    /**
     * 失败明细。
     *
     * @param skuId       失败的 SKU
     * @param warehouseId 仓库 ID
     * @param requested   请求数量
     * @param available   实际可用数量
     * @param reason      失败原因
     */
    public record Failure(String skuId, String warehouseId, int requested, int available, String reason) {
    }

    public static StockOperationResult ok(String bizNo, String transactionId, boolean idempotent) {
        return new StockOperationResult(bizNo, true, transactionId, idempotent, List.of());
    }
}
