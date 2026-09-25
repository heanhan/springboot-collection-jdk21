package com.example.dynamic.jpa.system.config;

import com.alibaba.ttl.threadpool.TtlExecutors;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步执行配置：{@code @Async} 默认执行器经 {@link TtlExecutors} 包装，
 * 使 {@link LoginInfoHolder} 中的租户上下文（TransmittableThreadLocal）能正确传播到异步线程，
 * 避免异步任务因丢失租户上下文而静默回退系统库。
 *
 * <p>说明：{@code @Scheduled} 定时任务无提交方上下文可继承，须在任务内显式
 * {@code LoginInfoHolder.runAsSystem(...)} 或按租户逐个 {@code setTenant(...)}。</p>
 *
 * @author zhaojh
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("tenant-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        // 用 TTL 包装，任务提交时快照租户上下文并在执行线程回放
        return TtlExecutors.getTtlExecutor(executor);
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}
