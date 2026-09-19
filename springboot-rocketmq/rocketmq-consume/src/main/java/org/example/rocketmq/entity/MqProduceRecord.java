package org.example.rocketmq.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;
import org.example.rocketmq.enums.ProduceStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 生产端消息表：一条记录代表「一条被生产的消息」的发送生命周期。
 *
 * <p><b>用途</b>：在消息<b>发送前</b>落库（PENDING），发送后回写结果（SUCCESS/FAILED），
 * 实现生产端可靠发送与可追溯：</p>
 * <ol>
 *     <li><b>先落库再发送</b>：即便发送过程中应用宕机，PENDING 记录也已存在，可由补偿任务扫描重发；</li>
 *     <li><b>发送失败入重试表</b>：失败时除置本表 FAILED 外，另写入 {@code mq_message_retry}(type=PRODUCE)，
 *         保存原始报文，由调度器重新生产；</li>
 *     <li><b>幂等键</b>：{@code (bizKey, topic)} 唯一索引，生产者通过 {@code RocketMQHeaders.KEYS} 设置业务 Key。</li>
 * </ol>
 *
 * <p><b>职责边界</b>：本表<b>只</b>记录生产端状态；消费端状态见 {@link MqConsumeRecord}，
 * 失败重试统一见 {@link MqMessageRetry}。三者通过 {@code (bizKey, topic)} 关联。</p>
 *
 * <pre>
 * CREATE TABLE `mq_produce_record` (
 *   `id`               BIGINT       NOT NULL AUTO_INCREMENT,
 *   `biz_key`          VARCHAR(128) NOT NULL COMMENT '业务幂等键（消息 Key）',
 *   `topic`            VARCHAR(128) NOT NULL COMMENT '消息主题',
 *   `tags`             VARCHAR(64)           COMMENT '消息标签',
 *   `body`             TEXT                  COMMENT '消息体(JSON)，生产重试时直接取此重发',
 *   `producer_group`   VARCHAR(128)          COMMENT '生产者组',
 *   `msg_id`           VARCHAR(128)          COMMENT 'RocketMQ msgId（发送成功后回填）',
 *   `produce_status`   VARCHAR(16)  NOT NULL COMMENT 'PENDING/SUCCESS/FAILED',
 *   `produce_time`     DATETIME              COMMENT '发送完成时间',
 *   `produce_error`    VARCHAR(1000)         COMMENT '发送失败原因',
 *   `created_time`     DATETIME,
 *   `updated_time`     DATETIME,
 *   PRIMARY KEY (`id`),
 *   UNIQUE KEY `uk_biz_topic` (`biz_key`, `topic`),
 *   KEY `idx_produce_status` (`produce_status`)
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
 * </pre>
 *
 * @author demo
 */
@Data
@Entity
@Table(
        name = "mq_produce_record",
        indexes = {
                // 幂等键：同一 Topic 下同一业务键只允许存在一条生产记录
                @Index(name = "uk_biz_topic", columnList = "biz_key,topic", unique = true),
                // 便于按生产状态扫描待补偿的消息（PENDING 过久 / FAILED）
                @Index(name = "idx_produce_status", columnList = "produce_status")
        }
)
public class MqProduceRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 业务幂等键：由生产者通过 RocketMQHeaders.KEYS 显式设置 */
    @Column(name = "biz_key", length = 128, nullable = false)
    private String bizKey;

    /** 消息主题 */
    @Column(name = "topic", length = 128, nullable = false)
    private String topic;

    /** 消息标签 */
    @Column(name = "tags", length = 64)
    private String tags;

    /** 消息体（JSON 字符串）：生产重试时凭此重新发送 */
    @Lob
    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    /** 生产者组 */
    @Column(name = "producer_group", length = 128)
    private String producerGroup;

    /** RocketMQ 消息 ID：发送成功后回填 */
    @Column(name = "msg_id", length = 128)
    private String msgId;

    /** 生产状态：PENDING(已落库待发送) / SUCCESS(broker 已接收) / FAILED(发送失败) */
    @Enumerated(EnumType.STRING)
    @Column(name = "produce_status", length = 16, nullable = false)
    private ProduceStatus produceStatus;

    /** 发送完成时间（成功或失败均写入） */
    @Column(name = "produce_time")
    private LocalDateTime produceTime;

    /** 发送失败原因（截断保存） */
    @Column(name = "produce_error", length = 1000)
    private String produceError;

    /** 创建时间（消息落库时间） */
    @Column(name = "created_time")
    private LocalDateTime createdTime;

    /** 更新时间 */
    @Column(name = "updated_time")
    private LocalDateTime updatedTime;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdTime = now;
        this.updatedTime = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedTime = LocalDateTime.now();
    }
}
