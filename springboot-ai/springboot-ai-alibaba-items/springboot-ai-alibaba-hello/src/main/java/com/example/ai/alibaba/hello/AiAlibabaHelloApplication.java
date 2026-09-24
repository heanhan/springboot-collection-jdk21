package com.example.ai.alibaba.hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring AI Alibaba 入门示例启动类。
 *
 * <p>基于阿里云百炼（DashScope）平台，演示如何使用 Spring AI 的
 * {@code ChatClient} 与大模型进行最简对话。</p>
 */
@SpringBootApplication
public class AiAlibabaHelloApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiAlibabaHelloApplication.class, args);
    }
}
