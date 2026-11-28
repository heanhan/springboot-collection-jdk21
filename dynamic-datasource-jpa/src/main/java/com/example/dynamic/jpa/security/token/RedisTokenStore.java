package com.example.dynamic.jpa.security.token;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "app.token.store", havingValue = "redis")
public class RedisTokenStore implements TokenStore {

    private static final String PREFIX = "dynjpa:auth:";
    private static final String CONSUMED = "CONSUMED";
    private static final String REPLAY = "REPLAY";
    private record StoredRefreshToken(RefreshTokenInfo info, String tokenHash) {
    }
    private record RefreshTokenFamily(String userId, String familyId) {
    }

    private static final String CONSUME_SCRIPT = """
            local data = redis.call('GET', KEYS[1])
            if not data or data == 'CONSUMED' then
                local familyData = redis.call('GET', KEYS[2])
                if familyData then
                    local family = cjson.decode(familyData)
                    local familyKey = 'dynjpa:auth:refresh-family:' .. family.userId .. ':' .. family.familyId
                    local members = redis.call('SMEMBERS', familyKey)
                    for _, tokenId in ipairs(members) do
                        local stored = redis.call('GET', 'dynjpa:auth:refresh:' .. family.userId .. ':' .. tokenId)
                        if stored then
                            local meta = cjson.decode(stored)
                            redis.call('SET', 'dynjpa:auth:refresh-lookup:' .. meta.tokenHash, 'CONSUMED', 'KEEPTTL')
                            redis.call('DEL', 'dynjpa:auth:refresh-family-map:' .. meta.tokenHash)
                        end
                        redis.call('DEL', 'dynjpa:auth:refresh:' .. family.userId .. ':' .. tokenId)
                    end
                    redis.call('DEL', familyKey, 'dynjpa:auth:refresh-index:' .. family.userId, KEYS[2])
                    return 'REPLAY'
                end
                return false
            end
            local meta = cjson.decode(data)
            redis.call('DEL', 'dynjpa:auth:refresh:' .. meta.userId .. ':' .. meta.tokenId)
            redis.call('SREM', 'dynjpa:auth:refresh-index:' .. meta.userId, meta.tokenId)
            redis.call('SET', KEYS[1], 'CONSUMED', 'KEEPTTL')
            return data
            """;

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final DefaultRedisScript<String> consumeScript;

    public RedisTokenStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.consumeScript = new DefaultRedisScript<>(CONSUME_SCRIPT, String.class);
    }

    @Override
    public void saveRefreshToken(String userId, String tokenId, String username, String token,
                                 String familyId, int tokenVersion, Duration ttl) {
        String hash = hash(token);
        RefreshTokenInfo info = new RefreshTokenInfo(userId, tokenId, username, familyId, tokenVersion);
        String json = write(info);
        Duration replayTtl = ttl.multipliedBy(2);
        redis.opsForValue().set(refreshKey(userId, tokenId), writeStored(info, hash), ttl);
        redis.opsForValue().set(lookupKey(hash), json, ttl);
        redis.opsForValue().set(familyMapKey(hash), writeValue(new RefreshTokenFamily(userId, familyId)), replayTtl);
        redis.opsForSet().add(indexKey(userId), tokenId);
        redis.expire(indexKey(userId), replayTtl);
        redis.opsForSet().add(familyKey(userId, familyId), tokenId);
        redis.expire(familyKey(userId, familyId), replayTtl);
    }

    @Override
    public Optional<RefreshTokenInfo> findRefreshToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return read(redis.opsForValue().get(lookupKey(hash(token))));
    }

    @Override
    public Optional<RefreshTokenInfo> consumeRefreshToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String hash = hash(token);
        String result = redis.execute(consumeScript,
                java.util.List.of(lookupKey(hash), familyMapKey(hash)));
        if (REPLAY.equals(result)) {
            throw new RefreshTokenReplayException();
        }
        return read(result);
    }

    @Override
    public void deleteRefreshToken(String userId, String tokenId) {
        String json = redis.opsForValue().getAndDelete(refreshKey(userId, tokenId));
        redis.opsForSet().remove(indexKey(userId), tokenId);
        if (json != null) {
            try {
                StoredRefreshToken stored = objectMapper.readValue(json, StoredRefreshToken.class);
                redis.opsForValue().set(lookupKey(stored.tokenHash()), CONSUMED,
                        Duration.ofDays(14));
                redis.delete(familyMapKey(stored.tokenHash()));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Refresh token 存储数据损坏", e);
            }
        }
    }

    @Override
    public void deleteAllRefreshTokens(String userId) {
        Set<String> tokenIds = redis.opsForSet().members(indexKey(userId));
        if (tokenIds != null) {
            for (String tokenId : tokenIds) {
                deleteRefreshToken(userId, tokenId);
            }
        }
        redis.delete(indexKey(userId));
    }

    @Override
    public void blacklistAccessToken(String jti, Duration ttl) {
        if (jti == null || ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        redis.opsForValue().set(PREFIX + "blacklist:" + jti, "1", ttl);
    }

    @Override
    public boolean isAccessTokenBlacklisted(String jti) {
        return jti != null && Boolean.TRUE.equals(redis.hasKey(PREFIX + "blacklist:" + jti));
    }

    private Optional<RefreshTokenInfo> read(String json) {
        if (json == null || CONSUMED.equals(json)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, RefreshTokenInfo.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Refresh token 存储数据损坏", e);
        }
    }

    private String write(RefreshTokenInfo info) {
        return writeValue(info);
    }

    private String writeStored(RefreshTokenInfo info, String tokenHash) {
        return writeValue(new StoredRefreshToken(info, tokenHash));
    }

    private String writeValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Refresh token 序列化失败", e);
        }
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String refreshKey(String userId, String tokenId) {
        return PREFIX + "refresh:" + userId + ":" + tokenId;
    }

    private String lookupKey(String hash) {
        return PREFIX + "refresh-lookup:" + hash;
    }

    private String familyMapKey(String hash) {
        return PREFIX + "refresh-family-map:" + hash;
    }

    private String indexKey(String userId) {
        return PREFIX + "refresh-index:" + userId;
    }

    private String familyKey(String userId, String familyId) {
        return PREFIX + "refresh-family:" + userId + ":" + familyId;
    }
}
