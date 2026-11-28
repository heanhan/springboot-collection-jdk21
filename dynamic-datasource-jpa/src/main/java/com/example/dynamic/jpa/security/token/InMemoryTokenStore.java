package com.example.dynamic.jpa.security.token;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "app.token.store", havingValue = "memory")
public class InMemoryTokenStore implements TokenStore {

    private record Entry(RefreshTokenInfo info, long expireAt) {
        boolean expired() {
            return System.currentTimeMillis() > expireAt;
        }
    }

    private final Map<String, Entry> refresh = new ConcurrentHashMap<>();
    private final Map<String, String> lookup = new ConcurrentHashMap<>();
    private final Map<String, Entry> consumed = new ConcurrentHashMap<>();
    private final Map<String, Entry> blacklist = new ConcurrentHashMap<>();

    @Override
    public void saveRefreshToken(String userId, String tokenId, String username, String token,
                                 String familyId, int tokenVersion, Duration ttl) {
        long expireAt = System.currentTimeMillis() + ttl.toMillis();
        String hash = hash(token);
        RefreshTokenInfo info = new RefreshTokenInfo(userId, tokenId, username, familyId, tokenVersion);
        refresh.put(userId + ":" + tokenId, new Entry(info, expireAt));
        lookup.put(hash, userId + ":" + tokenId);
        consumed.remove(hash);
    }

    @Override
    public Optional<RefreshTokenInfo> findRefreshToken(String token) {
        String key = lookup.get(hash(token));
        Entry entry = key == null ? null : refresh.get(key);
        return valid(entry).map(Entry::info);
    }

    @Override
    public synchronized Optional<RefreshTokenInfo> consumeRefreshToken(String token) {
        String hash = hash(token);
        Entry replay = valid(consumed.get(hash)).orElse(null);
        if (replay != null) {
            deleteAllRefreshTokens(replay.info().userId());
            throw new RefreshTokenReplayException();
        }
        String key = lookup.get(hash);
        Entry entry = key == null ? null : valid(refresh.remove(key)).orElse(null);
        if (entry == null) {
            return Optional.empty();
        }
        consumed.put(hash, new Entry(entry.info(), System.currentTimeMillis() + Duration.ofDays(14).toMillis()));
        return Optional.of(entry.info());
    }

    @Override
    public void deleteRefreshToken(String userId, String tokenId) {
        refresh.remove(userId + ":" + tokenId);
    }

    @Override
    public void deleteAllRefreshTokens(String userId) {
        refresh.entrySet().removeIf(entry -> entry.getValue().info().userId().equals(userId));
    }

    @Override
    public void blacklistAccessToken(String jti, Duration ttl) {
        if (jti == null || ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        blacklist.put(jti, new Entry(null, System.currentTimeMillis() + ttl.toMillis()));
    }

    @Override
    public boolean isAccessTokenBlacklisted(String jti) {
        Entry entry = blacklist.get(jti);
        if (entry == null || entry.expired()) {
            blacklist.remove(jti);
            return false;
        }
        return true;
    }

    private Optional<Entry> valid(Entry entry) {
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expired()) {
            return Optional.empty();
        }
        return Optional.of(entry);
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
