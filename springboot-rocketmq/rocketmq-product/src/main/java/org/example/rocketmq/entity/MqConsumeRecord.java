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
import org.example.rocketmq.enums.ConsumeStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 本地消息表：一条记录代表「一条被消费的消息」的落库快照。
 *
 * <p><b>用途</b>：把 RocketMQ 收到的消息持久化到 MySQL，从而实现：</p>
 * <ol>
 *     <li><b>幂等去重</b>：{@code (bizKey, topic)} 建立唯一索引，重复投递的消息插入失败即视为已处理；</li>
 *     <li><b>失败记录</b>：消费失败时保存 {@code status=FAILED}、{@code errorMsg}、{@code retryCount}；</li>
 *     <li><b>数据库重试</b>：定时任务扫描 {@code FAILED} 且到期的记录，重新执行业务；</li>
 *     <li><b>死信兜底</b>：重试达上限置为 {@code DEAD}，便于人工排查。</li>
 * </ol>
 *
 * <p>建表方式：默认由 JPA {@code ddl-auto=update} 自动创建；生产环境建议用下方 DDL 脚本管理。</p>
 *
 * <pre>
 * CREATE TABLE `mq_consume_record` (
 *   `id`              BIGINT       NOT NULL AUTO_INCREMENT,
 *   `biz_key`         VARCHAR(128) NOT NULL COMMENT '业务幂等键（消息 Key）',
 *   `topic`           VARCHAR(128) NOT NULL COMMENT '消息主题',
 *   `msg_id`          VARCHAR(128)          COMMENT 'RocketMQ msgId',
 *   `tags`            VARCHAR(64)           COMMENT '消息标签',
 *   `consumer_group`  VARCHAR(128)          COMMENT '消费组',
 *   `body`            TEXT                  COMMENT '消息体(JSON)',
 *   `status`          VARCHAR(16)  NOT NULL COMMENT 'CONSUMING/SUCCESS/FAILED/DEAD',
 *   `retry_count`     INT          NOT NULL DEFAULT 0 COMMENT '已重试次数',
 *   `max_retry`       INT          NOT NULL DEFAULT 3 COMMENT '最大重试次数',
 *   `next_retry_time` DATETIME              COMMENT '下次重试时间',
 *   `error_msg`       VARCHAR(1000)         COMMENT '最后一次错误信息',
 *   `created_time`    DATETIME              COMMENT '创建时间',
 *   `updated_time`    DATETIME              COMMENT '更新时间',
 *   PRIMARY KEY (`id`),
 *   UNIQUE KEY `uk_biz_topic_group` (`biz_key`, `topic`, `consumer_group`),
 *   KEY `idx_status_retry` (`status`, `next_retry_time`)
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
 * </pre>
 *
 * @author demo
 */
@Data
@Entity
@Table(
        name = "mq_consume_record",
        indexes = {
                // 幂等核心：同一 Topic 下、同一消费组内，同一业务键只允许存在一条记录。
                // 之所以带上 consumer_group：同一条消息可能被多个消费组各消费一次（如 TOPIC_BASIC
                // 同时被「基础并发组」与「生命周期组」订阅），每个组都应有独立的消费状态与幂等判断。
                @Index(name = "uk_biz_topic_group", columnList = "biz_key,topic,consumer_group", unique = true),
                // 重试扫描：按状态 + 下次重试时间快速捞出待重试记录
                @Index(name = "idx_status_retry", columnList = "status,next_retry_time")
        }
)
public class MqConsumeRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 业务幂等键：优先取消息 Key（RocketMQHeaders.KEYS），无 Key 时退化为 msgId */
    @Column(name = "biz_key", length = 128, nullable = false)
    private String bizKey;

    /** 消息主题 */
    @Column(name = "topic", length = 128, nullable = false)
    private String topic;

    /** RocketMQ 消息 ID */
    @Column(name = "msg_id", length = 128)
    private String msgId;

    /** 消息标签 */
    @Column(name = "tags", length = 64)
    private String tags;

    /** 消费组 */
    @Column(name = "consumer_group", length = 128)
    private String consumerGroup;

    /** 消息体（JSON 字符串），数据库重试时凭此重新执行业务 */
    @Lob
    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    /** 消费状态，以字符串存储，便于在数据库中直接阅读 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private ConsumeStatus status;

    /** 已重试次数（不含首次消费）：每次失败 +1 */
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    /** 最大重试次数：达到后置为 DEAD */
    @Column(name = "max_retry", nullable = false)
    private Integer maxRetry = 3;

    /** 下次可重试时间：定时任务只捞取 next_retry_time <= now 的失败记录 */
    @Column(name = "next_retry_time")
    private LocalDateTime nextRetryTime;

    /** 最后一次失败的错误信息（截断保存，避免过长） */
    @Column(name = "error_msg", length = 1000)
    private String errorMsg;

    /** 创建时间 */
    @Column(name = "created_time")
    private LocalDateTime createdTime;

    /** 更新时间 */
    @Column(name = "updated_time")
    private LocalDateTime updatedTime;

    /** 插入前自动填充创建/更新时间 */
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdTime = now;
        this.updatedTime = now;
    }

    /** 更新前自动刷新更新时间 */
    @PreUpdate
    public void preUpdate() {
        this.updatedTime = LocalDateTime.now();
    }
}
