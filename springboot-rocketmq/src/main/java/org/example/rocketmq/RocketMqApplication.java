package org.example.rocketmq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 消息队列的启动类
 *
 */
@SpringBootApplication
public class RocketMqApplication
{
    public static void main(String[] args) {
        SpringApplication.run(RocketMqApplication.class,args);
    }
}
