package org.example.rocketmq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * RocketMQ 消费者服务启动类（rocketmq-consume 模块）。
 *
 * <p>本模块聚焦「消费端」全部场景：并发消费 / 顺序消费 / 广播消费 / Tag 过滤 / 失败重试 /
 * 延迟消费 / 批量消费 / 事务消费 / 生命周期定制（手动 ACK），并配套「本地消息表」的
 * 消费端幂等落库、失败重试与死信处理。</p>
 *
 * <p>对应的生产者示例已拆分至 {@code rocketmq-product} 模块，可独立部署运行。</p>
 *
 * @author demo
 */
// @EnableScheduling：开启定时任务能力（当前 MessageRetryScheduler 的 @Scheduled 默认注释，保留注解便于按需启用）
@EnableScheduling
@SpringBootApplication
public class ConsumeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsumeApplication.class, args);
        System.out.println("""

                ==========================================================
                  RocketMQ 消费者服务启动成功！
                  本模块不对外提供业务 REST 接口，仅通过 @RocketMQMessageListener 监听消息。
                  如需触发消费，请启动 rocketmq-product 模块并访问其 /rocketmq-demo/producer/index。
                ==========================================================
                """);
    }
}
