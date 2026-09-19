package com.example.ddd.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 库存仓库服务启动类。
 *
 * <p><b>限界上下文：Inventory &amp; Warehouse</b> — 预占 / 实扣 / 释放 / 库存流水。</p>
 *
 * @author ddd-learning
 */
@SpringBootApplication(scanBasePackages = {
        "com.example.ddd.inventory",
        "com.example.ddd.common"
})
@EnableFeignClients(basePackages = "com.example.ddd.contract")
@EnableAsync
public class InventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryApplication.class, args);
    }
}
