package com.example.ddd.inventory.application.port;

/** 应用端口：序列化同业务号操作，记录先释放后预占的取消屏障，防止悬挂预占。 */
public interface StockOperationGuard {
    String lock(String bizNo);
    void state(String bizNo,String state);
}
