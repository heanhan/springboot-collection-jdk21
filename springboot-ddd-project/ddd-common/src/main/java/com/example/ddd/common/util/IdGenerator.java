package com.example.ddd.common.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 雪花算法 (Snowflake) ID 生成器。
 *
 * <p><b>结构 (64 bit, 有符号 long, 首位固定为 0)：</b>
 * <pre>
 *  0 | 41 bit 时间戳 (毫秒, 相对自定义纪元) | 10 bit 机器 ID | 12 bit 序列号
 * </pre>
 * 支持每毫秒每机器生成 4096 个 ID，理论上 69 年不重复。</p>
 *
 * <p><b>为什么不用 UUID？</b>
 * UUID 是字符串且无序，作为数据库主键会导致 B+Tree 频繁分裂、写放大严重。
 * 雪花 ID 是<b>趋势递增的 long</b>，对 InnoDB 主键友好，且携带时间信息便于分片。</p>
 *
 * <p><b>机器 ID 分配：</b>
 * 学习项目采用"IP 后两段 + 随机"生成，生产环境应通过 ZooKeeper / Nacos / 配置中心统一分配。</p>
 *
 * <p><b>线程安全：</b>
 * 使用 synchronized 保证同一毫秒内序列号递增；不同毫秒通过时间戳区分。</p>
 *
 * @author ddd-learning
 */
public final class IdGenerator {

    /** 自定义纪元：2024-01-01 00:00:00 UTC，可用 69 年 */
    private static final long EPOCH = LocalDateTime.of(2024, 1, 1, 0, 0)
            .atZone(ZoneId.of("UTC")).toInstant().toEpochMilli();

    private static final long WORKER_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);   // 1023
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);    // 4095

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;                       // 12
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;      // 22

    /** 单例实例 */
    private static final IdGenerator INSTANCE = new IdGenerator(resolveWorkerId());

    private final long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    private IdGenerator(long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("workerId 超出范围 [0, " + MAX_WORKER_ID + "]");
        }
        this.workerId = workerId;
    }

    /**
     * 生成下一个 long 型 ID。
     */
    public static long nextId() {
        return INSTANCE.generate();
    }

    /**
     * 生成下一个字符串型 ID（业务层通常用字符串，避免 JS 精度丢失）。
     */
    public static String nextIdStr() {
        return Long.toString(INSTANCE.generate());
    }

    private synchronized long generate() {
        long timestamp = System.currentTimeMillis();

        // 时钟回拨保护：直接等待或抛异常
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {
                // 短暂回拨，等待
                try {
                    wait(offset << 1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("等待时钟恢复被中断", e);
                }
                timestamp = System.currentTimeMillis();
                if (timestamp < lastTimestamp) {
                    throw new IllegalStateException("时钟回拨，拒绝生成 ID，回拨 " + (lastTimestamp - timestamp) + "ms");
                }
            } else {
                throw new IllegalStateException("时钟严重回拨 " + offset + "ms，拒绝生成 ID");
            }
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // 当前毫秒序列号用尽，等待下一毫秒
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * 解析当前机器的 workerId：优先取 IP 后两段的低 10 bit，失败则随机。
     */
    private static long resolveWorkerId() {
        try {
            InetAddress address = InetAddress.getLocalHost();
            byte[] ipBytes = address.getAddress();
            long id = ((ipBytes[2] & 0xFF) << 8 | (ipBytes[3] & 0xFF)) & MAX_WORKER_ID;
            return id == 0 ? new SecureRandom().nextInt((int) MAX_WORKER_ID) : id;
        } catch (UnknownHostException e) {
            return new SecureRandom().nextInt((int) MAX_WORKER_ID);
        }
    }

    /**
     * 供测试使用的可重置序列（生产禁用）。
     */
    static AtomicLong testOnlyCounter() {
        return new AtomicLong(INSTANCE.sequence);
    }
}
