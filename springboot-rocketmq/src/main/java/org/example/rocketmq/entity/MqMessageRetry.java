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
import org.example.rocketmq.enums.RetryStatus;
import org.example.rocketmq.enums.RetryType;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一消息重试表：承载「生产失败」与「消费失败」两类待重试消息。
 *
 * <p><b>用途</b>：把发送失败 / 消费失败的消息<b>连同原始报文</b>独立落库，
 * 由 {@code MessageRetryScheduler} 定时扫描并按 {@link RetryType} 走不同重放路径：</p>
 * <ul>
 *     <li>{@code PRODUCE}：取 {@link #body} 原始报文，调用 RocketMQTemplate <b>重新发送</b>（再生产一次）；</li>
 *     <li>{@code CONSUME}：取 {@link #body} 原始报文，反查业务处理器 <b>重新执行业务</b>（再消费一次）。</li>
 * </ul>
 *
 * <p><b>为什么独立成表</b>：① 与业务记录表（mq_produce_record / mq_consume_record）解耦，
 * 重试调度只扫描本表，索引小、效率高；② 用 {@link #retryType} 一张表区分两类失败，
 * 统一退避策略与死信处理；③ 保存原始报文，重试无需回查业务表，可直接重放。</p>
 *
 * <p><b>多实例并发控制</b>：调度器捞取后用「乐观抢占」
 * （{@code update ... set status=RETRYING where id=? and status=PENDING}）避免重复重试。</p>
 *
 * <pre>
 * CREATE TABLE `mq_message_retry` (
 *   `id`               BIGINT       NOT NULL AUTO_INCREMENT,
 *   `retry_type`       VARCHAR(16)  NOT NULL COMMENT 'PRODUCE/CONSUME',
 *   `biz_key`          VARCHAR(128) NOT NULL COMMENT '业务幂等键',
 *   `topic`            VARCHAR(128) NOT NULL COMMENT '消息主题',
 *   `tags`             VARCHAR(64)           COMMENT '消息标签',
 *   `msg_id`           VARCHAR(128)          COMMENT 'RocketMQ msgId（消费重试时有值）',
 *   `group_name`       VARCHAR(128)          COMMENT '生产者组或消费者组',
 *   `body`             TEXT                  COMMENT '原始消息体(JSON)，重试直接取此重放',
 *   `status`           VARCHAR(16)  NOT NULL COMMENT 'PENDING/RETRYING/SUCCESS/DEAD',
 *   `retry_count`      INT          NOT NULL DEFAULT 0,
 *   `max_retry`        INT          NOT NULL DEFAULT 3,
 *   `next_retry_time`  DATETIME              COMMENT '下次重试时间',
 *   `error_msg`        VARCHAR(1000)         COMMENT '最后一次失败原因',
 *   `source_record_id` BIGINT                COMMENT '来源记录ID（mq_produce_record/mq_consume_record 主键）',
 *   `created_time`     DATETIME,
 *   `updated_time`     DATETIME,
 *   PRIMARY KEY (`id`),
 *   KEY `idx_type_status_retry` (`retry_type`, `status`, `next_retry_time`),
 *   KEY `idx_biz_topic` (`biz_key`, `topic`)
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
 * </pre>
 *
 * @author demo
 */
@Data
@Entity
@Table(
        name = "mq_message_retry",
        indexes = {
                // 调度器扫描：按类型 + 状态 + 到期时间快速捞出待重试记录
                @Index(name = "idx_type_status_retry", columnList = "retry_type,status,next_retry_time"),
                // 按业务键排查
                @Index(name = "idx_biz_topic", columnList = "biz_key,topic")
        }
)
public class MqMessageRetry implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 重试类型：PRODUCE(生产失败重发) / CONSUME(消费失败重放) */
    @Enumerated(EnumType.STRING)
    @Column(name = "retry_type", length = 16, nullable = false)
    private RetryType retryType;

    /** 业务幂等键 */
    @Column(name = "biz_key", length = 128, nullable = false)
    private String bizKey;

    /** 消息主题 */
    @Column(name = "topic", length = 128, nullable = false)
    private String topic;

    /** 消息标签：生产重试时用于还原 destination(topic:tag) */
    @Column(name = "tags", length = 64)
    private String tags;

    /** RocketMQ 消息 ID：消费重试时有值；生产重试时发送成功后回填 */
    @Column(name = "msg_id", length = 128)
    private String msgId;

    /** 组名：生产重试时为生产者组，消费重试时为消费者组 */
    @Column(name = "group_name", length = 128)
    private String groupName;

    /** 原始消息体（JSON）：重试时直接取此重放，无需回查业务表 */
    @Lob
    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    /** 重试状态：PENDING/RETRYING/SUCCESS/DEAD */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private RetryStatus status = RetryStatus.PENDING;

    /** 已重试次数 */
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    /** 最大重试次数：达到后置 DEAD */
    @Column(name = "max_retry", nullable = false)
    private Integer maxRetry = 3;

    /** 下次可重试时间：调度器只捞取 next_retry_time <= now 的记录 */
    @Column(name = "next_retry_time")
    private LocalDateTime nextRetryTime;

    /** 最后一次失败原因（截断保存） */
    @Column(name = "error_msg", length = 1000)
    private String errorMsg;

    /** 来源记录 ID：关联 mq_produce_record 或 mq_consume_record 的主键，便于回溯 */
    @Column(name = "source_record_id")
    private Long sourceRecordId;

    /** 创建时间 */
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
