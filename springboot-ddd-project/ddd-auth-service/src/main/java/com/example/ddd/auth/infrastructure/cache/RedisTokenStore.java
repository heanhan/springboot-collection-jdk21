package com.example.ddd.auth.infrastructure.cache;

import com.example.ddd.auth.application.port.TokenStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Redis 实现的 Token 存储（{@link TokenStore} 的适配器）。
 *
 * <p><b>Key 设计：</b>
 * <ul>
 *   <li>{@code auth:refresh:{userId}:{tokenId}} = 完整 RefreshToken 字符串，TTL = 7 天</li>
 *   <li>{@code auth:refresh-index:{userId}} = Set of tokenId，用于批量删除该用户所有 RefreshToken</li>
 *   <li>{@code auth:blacklist:{tokenId}} = "1"，TTL = AccessToken 剩余秒数</li>
 *   <li>{@code auth:refresh-lookup:{tokenHash}} = "{userId}:{tokenId}"，反查用</li>
 * </ul>
 *
 * <p><b>为什么 RefreshToken 需要反查索引？</b>
 * 客户端刷新时只带 RefreshToken 字符串，服务端需要根据它找到 userId+tokenId，
 * 因此额外维护一个 {@code tokenHash -> (userId,tokenId)} 的映射。</p>
 *
 * <p><b>降级：</b>Redis 未启动时（本地开发），Token 存储不可用，登录会失败。
 * 生产环境应确保 Redis 高可用（Sentinel / Cluster）。</p>
 */
@Component
public class RedisTokenStore implements TokenStore {

    private static final Logger log = LoggerFactory.getLogger(RedisTokenStore.class);

    private static final String KEY_REFRESH = "auth:refresh:%s:%s";
    private static final String KEY_REFRESH_INDEX = "auth:refresh-index:%s";
    private static final String KEY_REFRESH_LOOKUP = "auth:refresh-lookup:%s";
    private static final String KEY_BLACKLIST = "auth:blacklist:%s";

    private final StringRedisTemplate redis;

    public RedisTokenStore(@Autowired(required = false) StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void saveRefreshToken(String userId, String tokenId, String token, Duration ttl) {
        if (redis == null) {
            log.warn("[Redis-DRY-RUN] saveRefreshToken userId={} tokenId={}", userId, tokenId);
            return;
        }
        String key = String.format(KEY_REFRESH, userId, tokenId);
        redis.opsForValue().set(key, token, ttl);
        // 反查索引
        redis.opsForValue().set(String.format(KEY_REFRESH_LOOKUP, token), userId + ":" + tokenId, ttl);
        // 用户下的 tokenId 集合，便于批量删除
        String indexKey = String.format(KEY_REFRESH_INDEX, userId);
        redis.opsForSet().add(indexKey, tokenId);
        redis.expire(indexKey, ttl);
    }

    @Override
    public Optional<RefreshTokenInfo> findRefreshToken(String refreshToken) {
        if (redis == null || refreshToken == null) return Optional.empty();
        String lookup = redis.opsForValue().get(String.format(KEY_REFRESH_LOOKUP, refreshToken));
        if (lookup == null) return Optional.empty();
        String[] parts = lookup.split(":", 2);
        if (parts.length != 2) return Optional.empty();
        // 二次校验：主键也存在（防止索引与主数据不一致）
        String mainKey = String.format(KEY_REFRESH, parts[0], parts[1]);
        if (!Boolean.TRUE.equals(redis.hasKey(mainKey))) return Optional.empty();
        return Optional.of(new RefreshTokenInfo(parts[0], parts[1]));
    }

    @Override
    public void deleteRefreshToken(String userId, String tokenId) {
        if (redis == null) return;
        String key = String.format(KEY_REFRESH, userId, tokenId);
        String token = redis.opsForValue().get(key);
        redis.delete(key);
        if (token != null) {
            redis.delete(String.format(KEY_REFRESH_LOOKUP, token));
        }
        redis.opsForSet().remove(String.format(KEY_REFRESH_INDEX, userId), tokenId);
    }

    @Override
    public void deleteAllRefreshTokens(String userId) {
        if (redis == null) return;
        String indexKey = String.format(KEY_REFRESH_INDEX, userId);
        Set<String> tokenIds = redis.opsForSet().members(indexKey);
        if (tokenIds != null) {
            for (String tokenId : tokenIds) {
                deleteRefreshToken(userId, tokenId);
            }
        }
        redis.delete(indexKey);
    }

    @Override
    public void blacklistAccessToken(String tokenId, Duration ttl) {
        if (redis == null || tokenId == null || ttl.isZero() || ttl.isNegative()) return;
        redis.opsForValue().set(String.format(KEY_BLACKLIST, tokenId), "1", ttl);
    }

    @Override
    public boolean isAccessTokenBlacklisted(String tokenId) {
        if (redis == null || tokenId == null) return false;
        return Boolean.TRUE.equals(redis.hasKey(String.format(KEY_BLACKLIST, tokenId)));
    }
}
