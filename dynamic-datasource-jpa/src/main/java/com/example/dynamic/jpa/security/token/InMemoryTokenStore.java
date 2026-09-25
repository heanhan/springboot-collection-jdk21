package com.example.dynamic.jpa.security.token;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 内存版 {@link TokenStore}，用于测试环境或无 Redis 时的降级。
 *
 * <p>非持久化、单进程可见；带 TTL 惰性过期。生产环境应使用 {@link RedisTokenStore}。</p>
 *
 * @author zhaojh
 */
@Component
@ConditionalOnProperty(name = "app.token.store", havingValue = "memory")
public class InMemoryTokenStore implements TokenStore {

    private record Entry(String value, long expireAt) {
        boolean expired() {
            return System.currentTimeMillis() > expireAt;
        }
    }

    /** refresh-lookup: token -> userId:tokenId:username */
    private final Map<String, Entry> lookup = new ConcurrentHashMap<>();
    /** refresh 主键: userId:tokenId -> username */
    private final Map<String, Entry> refresh = new ConcurrentHashMap<>();
    /** 黑名单: jti -> "1" */
    private final Map<String, Entry> blacklist = new ConcurrentHashMap<>();

    private static long expireAt(Duration ttl) {
        return System.currentTimeMillis() + (ttl == null ? 0 : ttl.toMillis());
    }

    @Override
    public void saveRefreshToken(String userId, String tokenId, String username, String token, Duration ttl) {
        long exp = expireAt(ttl);
        lookup.put(token, new Entry(userId + ":" + tokenId + ":" + username, exp));
        refresh.put(userId + ":" + tokenId, new Entry(username, exp));
    }

    @Override
    public Optional<RefreshTokenInfo> findRefreshToken(String token) {
        if (token == null) {
            return Optional.empty();
        }
        Entry entry = lookup.get(token);
        if (entry == null || entry.expired()) {
            lookup.remove(token);
            return Optional.empty();
        }
        String[] parts = entry.value().split(":", 3);
        if (parts.length < 3) {
            return Optional.empty();
        }
        Entry main = refresh.get(parts[0] + ":" + parts[1]);
        if (main == null || main.expired()) {
            return Optional.empty();
        }
        return Optional.of(new RefreshTokenInfo(parts[0], parts[1], parts[2]));
    }

    @Override
    public void deleteRefreshToken(String userId, String tokenId) {
        String mainKey = userId + ":" + tokenId;
        Entry main = refresh.remove(mainKey);
        if (main != null) {
            // 反查并删除对应 lookup
            lookup.entrySet().removeIf(e -> !e.getValue().expired()
                    && e.getValue().value().startsWith(userId + ":" + tokenId + ":"));
        }
    }

    @Override
    public void deleteAllRefreshTokens(String userId) {
        Set<String> tokenIds = refresh.keySet().stream()
                .filter(k -> k.startsWith(userId + ":"))
                .map(k -> k.substring(userId.length() + 1))
                .collect(Collectors.toSet());
        for (String tokenId : tokenIds) {
            deleteRefreshToken(userId, tokenId);
        }
    }

    @Override
    public void blacklistAccessToken(String jti, Duration ttl) {
        if (jti == null || ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        blacklist.put(jti, new Entry("1", expireAt(ttl)));
    }

    @Override
    public boolean isAccessTokenBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }
        Entry entry = blacklist.get(jti);
        if (entry == null) {
            return false;
        }
        if (entry.expired()) {
            blacklist.remove(jti);
            return false;
        }
        return true;
    }
}
