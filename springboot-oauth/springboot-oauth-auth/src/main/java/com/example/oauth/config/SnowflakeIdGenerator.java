package com.example.oauth.config;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于 Snowflake 算法的 Hibernate ID 生成器
 * <p>
 * 结构：1bit 符号位 + 41bit 时间戳 + 5bit 数据中心 + 5bit 机器ID + 12bit 序列号
 */
public class SnowflakeIdGenerator implements IdentifierGenerator {

    /** 起始时间戳 (2023-01-01 00:00:00) */
    private static final long TWEPOCH = 1672531200000L;

    private static final long WORKER_ID_BITS = 5L;
    private static final long DATA_CENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private final long workerId;
    private final long dataCenterId;
    private final AtomicLong sequence = new AtomicLong(0L);
    private volatile long lastTimestamp = -1L;

    public SnowflakeIdGenerator() {
        this(1L, 1L);
    }

    public SnowflakeIdGenerator(long workerId, long dataCenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("worker Id can't be greater than " + MAX_WORKER_ID + " or less than 0");
        }
        if (dataCenterId > MAX_DATA_CENTER_ID || dataCenterId < 0) {
            throw new IllegalArgumentException("datacenter Id can't be greater than " + MAX_DATA_CENTER_ID + " or less than 0");
        }
        this.workerId = workerId;
        this.dataCenterId = dataCenterId;
    }

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
        return nextId();
    }

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();

        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id for " + (lastTimestamp - timestamp) + " milliseconds");
        }

        if (lastTimestamp == timestamp) {
            long seq = (sequence.incrementAndGet()) & SEQUENCE_MASK;
            if (seq == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
            lastTimestamp = timestamp;
            return ((timestamp - TWEPOCH) << TIMESTAMP_LEFT_SHIFT)
                    | (dataCenterId << DATA_CENTER_ID_SHIFT)
                    | (workerId << WORKER_ID_SHIFT)
                    | seq;
        } else {
            sequence.set(0L);
            lastTimestamp = timestamp;
            return ((timestamp - TWEPOCH) << TIMESTAMP_LEFT_SHIFT)
                    | (dataCenterId << DATA_CENTER_ID_SHIFT)
                    | (workerId << WORKER_ID_SHIFT)
                    | sequence.getAndIncrement();
        }
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
