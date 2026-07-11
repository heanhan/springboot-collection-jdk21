package com.example.oauth.service.impl;

import com.example.oauth.service.LoginAttemptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 登录失败限制服务实现
 * <p>
 * 5 次失败锁定 30 分钟，基于 Redis 计数器
 */
@Slf4j
@Service
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private static final String LOGIN_FAIL_KEY_PREFIX = "oauth:login:fail:";

    /** 最大失败次数 */
    @Value("${security.login.max-fail-count:5}")
    private int maxFailCount;

    /** 锁定时间（分钟） */
    @Value("${security.login.lock-minutes:30}")
    private int lockMinutes;

    private final RedisTemplate<String, Object> redisTemplate;

    public LoginAttemptServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void recordFailedAttempt(String username) {
        String key = LOGIN_FAIL_KEY_PREFIX + username;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // 第一次失败，设置过期时间
            redisTemplate.expire(key, lockMinutes, TimeUnit.MINUTES);
        }
        log.warn("用户 [{}] 登录失败，当前失败次数: {}/{}", username, count, maxFailCount);

        if (count != null && count >= maxFailCount) {
            log.warn("用户 [{}] 登录失败次数超限，账号已被锁定 {} 分钟", username, lockMinutes);
        }
    }

    @Override
    public void resetAttempts(String username) {
        String key = LOGIN_FAIL_KEY_PREFIX + username;
        redisTemplate.delete(key);
    }

    @Override
    public boolean isLocked(String username) {
        String key = LOGIN_FAIL_KEY_PREFIX + username;
        Object count = redisTemplate.opsForValue().get(key);
        if (count == null) {
            return false;
        }
        int failCount;
        if (count instanceof Integer) {
            failCount = (Integer) count;
        } else if (count instanceof Long) {
            failCount = ((Long) count).intValue();
        } else if (count instanceof String) {
            failCount = Integer.parseInt((String) count);
        } else {
            return false;
        }
        return failCount >= maxFailCount;
    }

    @Override
    public int getRemainingAttempts(String username) {
        String key = LOGIN_FAIL_KEY_PREFIX + username;
        Object count = redisTemplate.opsForValue().get(key);
        if (count == null) {
            return maxFailCount;
        }
        int failCount;
        if (count instanceof Integer) {
            failCount = (Integer) count;
        } else if (count instanceof Long) {
            failCount = ((Long) count).intValue();
        } else if (count instanceof String) {
            failCount = Integer.parseInt((String) count);
        } else {
            return maxFailCount;
        }
        return Math.max(0, maxFailCount - failCount);
    }

    @Override
    public long getLockTimeRemaining(String username) {
        String key = LOGIN_FAIL_KEY_PREFIX + username;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.MINUTES);
        return ttl != null ? ttl : 0;
    }
}
