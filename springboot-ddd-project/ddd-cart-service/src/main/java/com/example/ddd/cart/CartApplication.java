package com.example.ddd.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 购物车服务启动类。
 *
 * <p><b>限界上下文：Shopping Cart</b> — 加购 / 结算预览。</p>
 *
 * @author ddd-learning
 */
@SpringBootApplication(scanBasePackages = {
        "com.example.ddd.cart",
        "com.example.ddd.common"
})
@EnableFeignClients(basePackages = "com.example.ddd.contract")
@EnableAsync
public class CartApplication {

    public static void main(String[] args) {
        SpringApplication.run(CartApplication.class, args);
    }
}
