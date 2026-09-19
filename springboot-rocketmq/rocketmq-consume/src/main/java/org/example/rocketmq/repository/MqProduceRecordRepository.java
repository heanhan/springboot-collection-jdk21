package org.example.rocketmq.repository;

import org.example.rocketmq.entity.MqProduceRecord;
import org.example.rocketmq.enums.ProduceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * 生产端消息表数据访问接口。
 *
 * <p><b>用途</b>：为「生产端落库、发送状态回写、补偿扫描、监控查询」提供持久化能力。
 * 本表只记录生产端状态；消费状态见 {@link MqConsumeRecordRepository}，重试见 MqMessageRetryRepository。</p>
 *
 * @author demo
 */
public interface MqProduceRecordRepository
        extends JpaRepository<MqProduceRecord, Long>, JpaSpecificationExecutor<MqProduceRecord> {

    /** 按业务幂等键 + 主题查询记录（唯一） */
    MqProduceRecord findByBizKeyAndTopic(String bizKey, String topic);

    /** 按生产状态统计数量：观察 PENDING / FAILED 是否有堆积 */
    long countByProduceStatus(ProduceStatus status);

    /** 查询最近 100 条生产记录（按创建时间倒序），供监控接口展示 */
    List<MqProduceRecord> findTop100ByOrderByCreatedTimeDesc();

    /** 按 Topic 查询最近 100 条记录（按创建时间倒序） */
    List<MqProduceRecord> findTop100ByTopicOrderByCreatedTimeDesc(String topic);
}
