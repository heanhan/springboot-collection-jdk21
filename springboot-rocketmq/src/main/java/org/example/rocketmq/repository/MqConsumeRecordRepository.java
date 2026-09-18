package org.example.rocketmq.repository;

import org.example.rocketmq.entity.MqConsumeRecord;
import org.example.rocketmq.enums.ConsumeStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 本地消息表数据访问接口。
 *
 * <p><b>用途</b>：为「幂等去重、失败记录、数据库重试」提供持久化能力。</p>
 *
 * @author demo
 */
public interface MqConsumeRecordRepository extends JpaRepository<MqConsumeRecord, Long> {

    /**
     * 按业务幂等键 + 主题查询记录（唯一）。
     * 用于重复投递时判断该消息是否已消费成功，从而做幂等跳过。
     */
    MqConsumeRecord findByBizKeyAndTopic(String bizKey, String topic);

    /**
     * 查询「待重试」的失败记录：状态为 FAILED 且下次重试时间已到期。
     * 按 next_retry_time 升序，优先处理等待最久的；通过 Pageable 限制单次条数。
     *
     * @param status 期望状态（固定传 FAILED）
     * @param now    当前时间
     * @param pageable 分页/限流参数
     */
    @Query("select r from MqConsumeRecord r "
            + "where r.status = :status and r.nextRetryTime is not null and r.nextRetryTime <= :now "
            + "order by r.nextRetryTime asc")
    List<MqConsumeRecord> findRetryable(@Param("status") ConsumeStatus status,
                                        @Param("now") LocalDateTime now,
                                        Pageable pageable);

    /**
     * 按状态统计数量，供监控接口展示（如 FAILED/DEAD 堆积情况）。
     */
    long countByStatus(ConsumeStatus status);

    /**
     * 查询最近若干条记录（按创建时间倒序），供监控接口展示。
     */
    List<MqConsumeRecord> findTop100ByOrderByCreatedTimeDesc();
}
