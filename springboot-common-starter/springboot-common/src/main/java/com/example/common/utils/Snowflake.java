package com.example.common.utils;

import java.text.MessageFormat;

public class Snowflake {
    // ==============================Fields===========================================
    /**
     * 开始时间戳 (2000-01-01 00:00:00)
     */
    private static final long TWEPOCH = 946656000000L;

    /**
     * 机器id所占的位数 5
     */
    private static final long WORKER_ID_BITS = 5L;

    /**
     * 数据标识id所占的位数 5
     */
    private static final long DATA_CENTER_ID_BITS = 5L;

    /**
     * 支持的最大机器id，结果是 31
     */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /**
     * 支持的最大数据标识id，结果是 31
     */
    private static final long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);

    /**
     * 序列在id中占的位数
     */
    private static final long SEQUENCE_BITS = 12L;

    /**
     * 机器ID向左移12位
     */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /**
     * 数据标识id向左移17位(12+5)
     */
    private static final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /**
     * 时间戳向左移22位(5+5+12)
     */
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;

    /**
     * 生成序列的掩码，这里为4095 (0b111111111111=0xfff=4095)
     */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /**
     * 步长 1024
     */
    private static final long STEP_SIZE = 1024;

    /**
     * unsigned int max value
     */
    private static final long UINT_MAX_VALUE = 0xffffffffL;

    /**
     * 工作机器ID(0~31)
     */
    private long workerId;

    /**
     * 工作机器ID 计数器
     */
    private long workerIdFlags = 0L;

    /**
     * 数据中心ID(0~31)
     */
    private long dataCenterId;

    /**
     * 数据中心ID 计数器
     */
    private long dataCenterIdFlags = 0L;

    /**
     * 毫秒内序列(0~4095)
     */
    private long sequence = 0L;

    /**
     * 毫秒内序列基数[0|1024|2048|3072]
     */
    private long basicSequence = 0L;

    /**
     * 上次生成ID的时间戳
     */
    private long lastTimestamp = -1L;

    /**
     * 工作模式
     */
    private final WorkMode workMode;

    public enum WorkMode { NON_SHARED, RATE_1024, RATE_4096; }

    //==============================Constructors=====================================

    public Snowflake() {
        this(0, 0, WorkMode.RATE_4096);
    }

    /**
     * 构造函数
     * @param workerId     工作ID (0~31)
     * @param dataCenterId 数据中心ID (0~31)
     */
    public Snowflake(long workerId, long dataCenterId) {
        this(workerId, dataCenterId, WorkMode.RATE_4096);
    }

    /**
     * 构造函数
     * @param workerId     工作ID (0~31)
     * @param dataCenterId 数据中心ID (0~31)
     * @param workMode     工作模式
     */
    public Snowflake(long workerId, long dataCenterId, WorkMode workMode) {
        this.workMode = workMode;
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(MessageFormat.format("worker Id can't be greater than {0} or less than 0", MAX_WORKER_ID));
        }
        if (dataCenterId > MAX_DATA_CENTER_ID || dataCenterId < 0) {
            throw new IllegalArgumentException(MessageFormat.format("datacenter Id can't be greater than {0} or less than 0", MAX_DATA_CENTER_ID));
        }
        this.workerId = workerId;
        this.workerIdFlags = setSpecifiedBitTo1(this.workerIdFlags, this.workerId);
        this.dataCenterId = dataCenterId;
        this.dataCenterIdFlags = setSpecifiedBitTo1(this.dataCenterIdFlags, this.dataCenterId);
    }

    // ==============================Methods==========================================

    /**
     * 获取机器id
     *
     * @return 所属机器的id
     */
    public long getWorkerId() {
        return workerId;
    }

    /**
     * 获取数据中心id
     *
     * @return 所属数据中心id
     */
    public long getDataCenterId() {
        return dataCenterId;
    }

    /**
     * 获得下一个ID (该方法是线程安全的)
     *
     * @return SnowflakeId
     */
    public synchronized long nextId() {
        long timestamp = timeGen();
        //如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
        if (timestamp < this.lastTimestamp) {
            if (timestamp > TWEPOCH) {
                if (WorkMode.NON_SHARED == this.workMode) {
                    nonSharedClockBackwards(timestamp);
                } else if (WorkMode.RATE_1024 == this.workMode) {
                    rate1024ClockBackwards(timestamp);
                } else {
                    throw new RuntimeException(MessageFormat.format("Clock moved backwards. Refusing to generate id for {0} milliseconds", lastTimestamp - timestamp));
                }
            } else {
                throw new RuntimeException(MessageFormat.format("Clock moved backwards. Refusing to generate id for {0} milliseconds", lastTimestamp - timestamp));
            }
        }
        //如果是同一时间生成的，则进行毫秒内序列
        if (this.lastTimestamp == timestamp) {
            this.sequence = (this.sequence + 1) & SEQUENCE_MASK;
            //毫秒内序列溢出
            if (this.sequence == 0) {
                //阻塞到下一个毫秒,获得新的时间戳
                timestamp = tilNextMillis(this.lastTimestamp);
            }
        }
        //时间戳改变，毫秒内序列重置
        else {
            this.sequence = this.basicSequence;
        }
        //上次生成ID的时间戳
        this.lastTimestamp = timestamp;
        //移位并通过或运算拼到一起组成64位的ID
        return ((timestamp - TWEPOCH) << TIMESTAMP_LEFT_SHIFT)
                | (this.dataCenterId << DATA_CENTER_ID_SHIFT)
                | (this.workerId << WORKER_ID_SHIFT)
                | this.sequence;
    }

    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     *
     * @param lastTimestamp 上次生成ID的时间戳
     * @return 当前时间戳
     */
    protected long tilNextMillis(long lastTimestamp) {
        long timestamp0;
        do {
            timestamp0 = timeGen();
        } while (timestamp0 <= lastTimestamp);
        return timestamp0;
    }

    /**
     * 返回以毫秒为单位的当前时间
     *
     * @return 当前时间(毫秒)
     */
    protected long timeGen() {
        return System.currentTimeMillis();
    }

    /**
     * 尝试解决时钟回拨<br>【* 仅用于 单机生成不对外 的情况 *】
     *
     * @param timestamp 当前时间戳
     * @return void
     */
    private void nonSharedClockBackwards(long timestamp) {
        if (this.dataCenterIdFlags >= UINT_MAX_VALUE && this.workerIdFlags >= UINT_MAX_VALUE) {
            throw new RuntimeException(MessageFormat.format("Clock moved backwards. Refusing to generate id for {0} milliseconds", lastTimestamp - timestamp));
        } else {
            //如果仅用于生成不重复的数值，尝试变更 dataCenterId 或 workerId 修复时钟回拨问题
            //先尝试变更 dataCenterId，当 dataCenterId 轮询一遍之后，尝试变更 workerId 并重置 dataCenterId
            if (this.dataCenterIdFlags >= UINT_MAX_VALUE) {
                if (++this.workerId > MAX_WORKER_ID) { this.workerId = 0L; }
                this.workerIdFlags = setSpecifiedBitTo1(this.workerIdFlags, this.workerId);
                // 重置 dataCenterId 和 dataCenterIdFlags
                this.dataCenterIdFlags = this.dataCenterId = 0L;
            } else {
                if (++this.dataCenterId > MAX_DATA_CENTER_ID) { this.dataCenterId = 0L; }
            }
            this.dataCenterIdFlags = setSpecifiedBitTo1(this.dataCenterIdFlags, this.dataCenterId);
            this.lastTimestamp = -1L;
        }
    }

    /**
     * 尝试解决时钟回拨<br>【* 仅用于每毫秒生成量 不大于 1024 的情况 *】
     *
     * @param timestamp 当前时间戳
     * @return void
     */
    private void rate1024ClockBackwards(long timestamp) {
        if (this.basicSequence > (SEQUENCE_MASK - STEP_SIZE)) {
            throw new RuntimeException(MessageFormat.format("Clock moved backwards. Refusing to generate id for {0} milliseconds", lastTimestamp - timestamp));
        } else {
            this.basicSequence += STEP_SIZE;
            this.lastTimestamp = -1L;
        }
    }

    /**
     * Set the specified bit to 1
     *
     * @param value    raw long value
     * @param index    bit index (From 0~31)
     * @return long value
     */
    private long setSpecifiedBitTo1(long value, long index) {
        return value |= (1L << index);
    }

    /**
     * Set the specified bit to 0
     *
     * @param value    raw long value
     * @param index    bit index (From 0~31)
     * @return long value
     */
    private long setSpecifiedBitTo0(long value, long index) {
        return value &= ~(1L << index);
    }

    /**
     * Get the specified bit
     * @param value    raw long value
     * @param index    bit index(From 0-31)
     * @return 0 or 1
     */
    private int getSpecifiedBit(long value, long index) {
        return (value & (1L << index)) == 0 ? 0 : 1;
    }

}

