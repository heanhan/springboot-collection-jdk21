package com.example.dynamic.jpa.common.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>TODO</p>
 *
 * @author zhaojh
 * @version V1.0.0
 * @date 2023/2/2
 */
public class MyCacheLock {

    private volatile Map<String, Object> map = new HashMap<>();
    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock(); // 读写锁

    public void put(String key, Object value) {
        // 写锁
        readWriteLock.writeLock().lock();
        try {
            System.out.println(Thread.currentThread().getName()+" 写入"+key); map.put(key,value); System.out.println(Thread.currentThread().getName()+" 写入成功!");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public void get(String key) {
        // 读锁
        readWriteLock.readLock().lock();
        try {
            System.out.println(Thread.currentThread().getName()+" 读取"+key); Object result = map.get(key); System.out.println(Thread.currentThread().getName()+" 读取结果:"+result);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            readWriteLock.readLock().unlock();
        }
    }
}
