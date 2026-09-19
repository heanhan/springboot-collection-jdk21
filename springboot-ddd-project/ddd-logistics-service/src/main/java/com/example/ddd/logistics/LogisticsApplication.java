package com.example.ddd.logistics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 物流履约服务启动类。
 *
 * <p><b>限界上下文：Logistics &amp; Fulfillment</b> — 发货单 / 运单 / 物流轨迹。</p>
 *
 * @author ddd-learning
 */
@SpringBootApplication(scanBasePackages = {
        "com.example.ddd.logistics",
        "com.example.ddd.common"
})
@EnableFeignClients(basePackages = "com.example.ddd.contract")
@EnableScheduling
@EnableAsync
public class LogisticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogisticsApplication.class, args);
    }
}
