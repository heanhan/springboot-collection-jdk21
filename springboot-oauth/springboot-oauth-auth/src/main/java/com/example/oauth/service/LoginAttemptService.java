package com.example.oauth.service;

/**
 * 登录失败限制服务接口
 * <p>
 * 基于 Redis 实现：5 次失败锁定 30 分钟
 */
public interface LoginAttemptService {

    /**
     * 记录登录失败
     *
     * @param username 用户名
     */
    void recordFailedAttempt(String username);

    /**
     * 登录成功后重置计数
     *
     * @param username 用户名
     */
    void resetAttempts(String username);

    /**
     * 检查用户是否被锁定
     *
     * @param username 用户名
     * @return true-已锁定
     */
    boolean isLocked(String username);

    /**
     * 获取剩余失败次数
     *
     * @param username 用户名
     * @return 剩余次数
     */
    int getRemainingAttempts(String username);

    /**
     * 获取锁定剩余时间（分钟）
     *
     * @param username 用户名
     * @return 锁定剩余时间，未锁定返回 0
     */
    long getLockTimeRemaining(String username);
}
