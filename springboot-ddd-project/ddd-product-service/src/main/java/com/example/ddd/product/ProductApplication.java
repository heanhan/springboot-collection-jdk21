package com.example.ddd.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 商品目录服务启动类。
 *
 * <p><b>限界上下文：Product Catalog</b> — SPU / SKU / 类目 / 品牌 / 上下架。</p>
 *
 * @author ddd-learning
 */
@SpringBootApplication(scanBasePackages = {
        "com.example.ddd.product",
        "com.example.ddd.common"
})
@EnableFeignClients(basePackages = "com.example.ddd.contract")
@EnableAsync
public class ProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductApplication.class, args);
    }
}
