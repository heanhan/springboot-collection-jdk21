package org.example.rocketmq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * RocketMQ 学习 Demo 启动类。
 *
 * <p>本模块演示 Spring Boot 3 + Apache RocketMQ（rocketmq-spring-boot-starter 2.3.1）
 * 的全部常见使用场景，包括：</p>
 * <ul>
 *     <li>生产者：同步 / 异步 / 单向 / 延迟 / 顺序 / 批量 / 事务 / 带 Tag&Key 发送</li>
 *     <li>消费者：并发消费 / 顺序消费 / 广播消费 / Tag 过滤 / 失败重试 / 延迟消费 /
 *         批量消费 / 事务消费 / 生命周期定制（手动控制）</li>
 * </ul>
 *
 * <p>启动后可通过 REST 接口（见 {@code producer.ProducerController}）逐个触发生产者场景，
 * 对应的消费者会自动打印日志，便于观察。</p>
 *
 * @author demo
 */
// @EnableScheduling：开启定时任务，用于「本地消息表」失败消息的数据库重试调度
@EnableScheduling
@SpringBootApplication
public class RocketMqApplication {

    public static void main(String[] args) {
        SpringApplication.run(RocketMqApplication.class, args);
        // 启动成功后打印访问入口，方便学习者快速定位测试地址
        System.out.println("""

                ==========================================================
                  RocketMQ 学习 Demo 启动成功！
                  接口根路径: http://localhost:11001/rocketmq-demo
                  测试导航页: http://localhost:11001/rocketmq-demo/producer/index
                ==========================================================
                """);
    }
}
