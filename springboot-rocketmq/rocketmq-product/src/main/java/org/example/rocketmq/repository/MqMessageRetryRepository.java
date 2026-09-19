package org.example.rocketmq.repository;

import org.example.rocketmq.entity.MqMessageRetry;
import org.example.rocketmq.enums.RetryStatus;
import org.example.rocketmq.enums.RetryType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 统一消息重试表数据访问接口。
 *
 * <p><b>用途</b>：为「失败消息落库、定时扫描重试、乐观抢占、状态统计」提供持久化能力。</p>
 *
 * @author demo
 */
public interface MqMessageRetryRepository
        extends JpaRepository<MqMessageRetry, Long>, JpaSpecificationExecutor<MqMessageRetry> {

    /**
     * 捞取到期待重试记录：status=PENDING 且 next_retry_time <= now，按到期时间升序。
     *
     * @param status   固定传 PENDING
     * @param now      当前时间
     * @param pageable 限制单轮条数
     */
    @Query("select r from MqMessageRetry r "
            + "where r.status = :status and r.nextRetryTime is not null and r.nextRetryTime <= :now "
            + "order by r.nextRetryTime asc")
    List<MqMessageRetry> findRetryable(@Param("status") RetryStatus status,
                                       @Param("now") LocalDateTime now,
                                       Pageable pageable);

    /**
     * 乐观抢占：仅当记录仍为 PENDING 时置为 RETRYING，返回受影响行数。
     * 多实例并发扫描时，只有一个实例能抢占成功（affected=1），避免重复重试。
     *
     * @return 1=抢占成功；0=已被其他实例抢占或状态已变
     */
    @Modifying
    @Transactional
    @Query("update MqMessageRetry r set r.status = :to, r.updatedTime = :now "
            + "where r.id = :id and r.status = :from")
    int compareAndSetStatus(@Param("id") Long id,
                            @Param("from") RetryStatus from,
                            @Param("to") RetryStatus to,
                            @Param("now") LocalDateTime now);

    /** 按类型 + 状态统计数量，供监控接口展示 */
    long countByRetryTypeAndStatus(RetryType retryType, RetryStatus status);

    /** 按状态统计数量 */
    long countByStatus(RetryStatus status);

    /** 查询最近 100 条重试记录（按创建时间倒序） */
    List<MqMessageRetry> findTop100ByOrderByCreatedTimeDesc();

    /** 按类型查询最近 100 条重试记录 */
    List<MqMessageRetry> findTop100ByRetryTypeOrderByCreatedTimeDesc(RetryType retryType);
}
