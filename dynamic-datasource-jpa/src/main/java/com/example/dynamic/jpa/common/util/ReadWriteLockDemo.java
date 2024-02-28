package com.example.dynamic.jpa.common.util;

/**
 * <p>TODO</p>
 *
 * @author zhaojh
 * @version V1.0.0
 * @date 2023/2/2
 */

/**
 * 多个线程同时读一个资源类没有任何问题，所以为了满足并发量，读取共享资源应该可以同时进行。 * 但是，如果有一个线程想去写共享资源，就不应该再有其他线程可以对该资源进行读或写。
 * 1. 读-读 可以共存
 * 2. 读-写 不能共存
 * 3. 写-写 不能共存
 */

public class ReadWriteLockDemo {

    public static void main(String[] args) {
        MyCacheLock myCache = new MyCacheLock(); // 写
        for (int i = 1; i <= 5; i++) {
            final int tempInt = i;
            new Thread(() -> {
                myCache.put(tempInt + "", tempInt + "");
            }, String.valueOf(i)).start();
        }

        //读
        for (int i = 1; i <= 5; i++) {
            final int tempInt = i;
            new Thread(() -> {
                myCache.get(tempInt + "");
            }, String.valueOf(i)).start();
        }
    }

}
