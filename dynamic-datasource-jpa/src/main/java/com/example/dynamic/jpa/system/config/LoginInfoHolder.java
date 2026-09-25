package com.example.dynamic.jpa.system.config;


import com.alibaba.ttl.TransmittableThreadLocal;
import com.example.dynamic.jpa.system.vo.LoginInfo;

import java.util.function.Supplier;

/**
 * @Author: zhaojh
 * @ClassName: LoginInfoHolder
 * @Description: 存放用户登录标识信息，作为动态数据源路由的租户上下文。
 *
 * <p>底层使用 {@link TransmittableThreadLocal}，配合 TTL 执行器（见 {@code AsyncConfig}）
 * 可在 {@code @Async}、线程池任务中正确传播租户上下文，避免异步场景静默丢失租户而回退系统库。</p>
 */
public class LoginInfoHolder {

    /**
     * 系统库租户键
     */
    public static final int SYSTEM_TENANT_ID = 0;

    private static final TransmittableThreadLocal<LoginInfo> CONTEXT = new TransmittableThreadLocal<>();

    public static void setTenant(LoginInfo loginInfo) {
        CONTEXT.set(loginInfo);
    }

    public static LoginInfo getTenant() {
        return CONTEXT.get();
    }

    public static boolean isPresent() {
        return CONTEXT.get() != null;
    }

    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 以系统库上下文（tenantId=0）执行，执行结束后恢复原上下文。
     * 用于启动注册、路由懒加载、集群广播监听等非请求线程访问系统库的场景。
     */
    public static void runAsSystem(Runnable action) {
        LoginInfo previous = CONTEXT.get();
        CONTEXT.set(systemContext());
        try {
            action.run();
        } finally {
            restore(previous);
        }
    }

    /**
     * 以系统库上下文（tenantId=0）执行并返回结果，执行结束后恢复原上下文。
     */
    public static <T> T callAsSystem(Supplier<T> action) {
        LoginInfo previous = CONTEXT.get();
        CONTEXT.set(systemContext());
        try {
            return action.get();
        } finally {
            restore(previous);
        }
    }

    private static LoginInfo systemContext() {
        LoginInfo system = new LoginInfo();
        system.setTenantId(SYSTEM_TENANT_ID);
        return system;
    }

    private static void restore(LoginInfo previous) {
        if (previous == null) {
            CONTEXT.remove();
        } else {
            CONTEXT.set(previous);
        }
    }
}
