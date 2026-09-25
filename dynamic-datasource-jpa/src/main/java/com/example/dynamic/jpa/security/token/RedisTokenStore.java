package com.example.dynamic.jpa.security.token;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * 基于 Redis 的 {@link TokenStore} 实现。
 *
 * <p>键结构：
 * <ul>
 *   <li>{@code dynjpa:auth:refresh:{userId}:{tokenId}} -> {@code username|token}</li>
 *   <li>{@code dynjpa:auth:refresh-lookup:{token}} -> {@code userId:tokenId:username}</li>
 *   <li>{@code dynjpa:auth:refresh-index:{userId}} -> Set(tokenId)</li>
 *   <li>{@code dynjpa:auth:blacklist:{jti}} -> "1"</li>
 * </ul>
 *
 * <p>当 Redis 不可达时所有操作降级为无状态模式（记录告警、不抛异常），
 * 保证 access token 校验链路不因缓存故障而整体不可用。</p>
 *
 * @author zhaojh
 */
@Component
@ConditionalOnProperty(name = "app.token.store", havingValue = "redis", matchIfMissing = true)
public class RedisTokenStore implements TokenStore {

    private static final Logger log = LoggerFactory.getLogger(RedisTokenStore.class);

    private static final String KEY_REFRESH = "dynjpa:auth:refresh:%s:%s";
    private static final String KEY_REFRESH_LOOKUP = "dynjpa:auth:refresh-lookup:%s";
    private static final String KEY_REFRESH_INDEX = "dynjpa:auth:refresh-index:%s";
    private static final String KEY_BLACKLIST = "dynjpa:auth:blacklist:%s";
    private static final String SEP = "\u0001";

    private final StringRedisTemplate redis;

    public RedisTokenStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void saveRefreshToken(String userId, String tokenId, String username, String token, Duration ttl) {
        try {
            redis.opsForValue().set(String.format(KEY_REFRESH, userId, tokenId), username + SEP + token, ttl);
            redis.opsForValue().set(String.format(KEY_REFRESH_LOOKUP, token), userId + ":" + tokenId + ":" + username, ttl);
            String indexKey = String.format(KEY_REFRESH_INDEX, userId);
            redis.opsForSet().add(indexKey, tokenId);
            redis.expire(indexKey, ttl);
        } catch (RuntimeException e) {
            log.warn("[Redis降级] 保存 refresh token 失败 userId={} tokenId={}: {}", userId, tokenId, e.getMessage());
        }
    }

    @Override
    public Optional<RefreshTokenInfo> findRefreshToken(String token) {
        if (token == null) {
            return Optional.empty();
        }
        try {
            String lookup = redis.opsForValue().get(String.format(KEY_REFRESH_LOOKUP, token));
            if (lookup == null) {
                return Optional.empty();
            }
            String[] parts = lookup.split(":", 3);
            if (parts.length < 3) {
                return Optional.empty();
            }
            String mainKey = String.format(KEY_REFRESH, parts[0], parts[1]);
            String value = redis.opsForValue().get(mainKey);
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(new RefreshTokenInfo(parts[0], parts[1], parts[2]));
        } catch (RuntimeException e) {
            log.warn("[Redis降级] 查询 refresh token 失败: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void deleteRefreshToken(String userId, String tokenId) {
        try {
            String key = String.format(KEY_REFRESH, userId, tokenId);
            String value = redis.opsForValue().get(key);
            redis.delete(key);
            if (value != null) {
                int idx = value.indexOf(SEP);
                String token = idx >= 0 ? value.substring(idx + 1) : null;
                if (token != null) {
                    redis.delete(String.format(KEY_REFRESH_LOOKUP, token));
                }
            }
            redis.opsForSet().remove(String.format(KEY_REFRESH_INDEX, userId), tokenId);
        } catch (RuntimeException e) {
            log.warn("[Redis降级] 删除 refresh token 失败 userId={} tokenId={}: {}", userId, tokenId, e.getMessage());
        }
    }

    @Override
    public void deleteAllRefreshTokens(String userId) {
        try {
            String indexKey = String.format(KEY_REFRESH_INDEX, userId);
            Set<String> tokenIds = redis.opsForSet().members(indexKey);
            if (tokenIds != null) {
                for (String tokenId : tokenIds) {
                    deleteRefreshToken(userId, tokenId);
                }
            }
            redis.delete(indexKey);
        } catch (RuntimeException e) {
            log.warn("[Redis降级] 清空用户 refresh token 失败 userId={}: {}", userId, e.getMessage());
        }
    }

    @Override
    public void blacklistAccessToken(String jti, Duration ttl) {
        if (jti == null || ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        try {
            redis.opsForValue().set(String.format(KEY_BLACKLIST, jti), "1", ttl);
        } catch (RuntimeException e) {
            log.warn("[Redis降级] access token 拉黑失败 jti={}: {}", jti, e.getMessage());
        }
    }

    @Override
    public boolean isAccessTokenBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redis.hasKey(String.format(KEY_BLACKLIST, jti)));
        } catch (RuntimeException e) {
            log.warn("[Redis降级] access token 黑名单查询失败 jti={}: {}", jti, e.getMessage());
            return false;
        }
    }
}
