package org.example.rocketmq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * RocketMQ 生产者服务启动类（rocketmq-product 模块）。
 *
 * <p>本模块聚焦「生产端」全部场景：同步 / 异步 / 单向 / 延迟 / 顺序 / 批量 / 事务 / 带 Tag&Key 发送，
 * 并配套「本地消息表」的生产端落库、失败重试与全链路查询接口。</p>
 *
 * <p>对应的消费者示例已拆分至 {@code rocketmq-consume} 模块，可独立部署运行。</p>
 *
 * <p>启动后可通过 REST 接口（见 {@code producer.ProducerController} /
 * {@code controller.ReliableController} / {@code controller.MessageRecordController}）逐个触发。</p>
 *
 * @author demo
 */
// @EnableScheduling：开启定时任务能力（当前 MessageRetryScheduler 的 @Scheduled 默认注释，保留注解便于按需启用）
@EnableScheduling
@SpringBootApplication
public class ProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductApplication.class, args);
        System.out.println("""

                ==========================================================
                  RocketMQ 生产者服务启动成功！
                  接口根路径: http://localhost:11001/rocketmq-demo
                  生产者导航页: http://localhost:11001/rocketmq-demo/producer/index
                  可靠消息导航页: http://localhost:11001/rocketmq-demo/reliable/index
                  生产消息全链路记录: http://localhost:11001/rocketmq-demo/message-record/index
                ==========================================================
                """);
    }
}
