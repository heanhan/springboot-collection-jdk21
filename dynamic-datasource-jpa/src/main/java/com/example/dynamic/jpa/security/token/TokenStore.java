package com.example.dynamic.jpa.security.token;

import java.time.Duration;
import java.util.Optional;

public interface TokenStore {

    void saveRefreshToken(String userId, String tokenId, String username, String token,
                          String familyId, int tokenVersion, Duration ttl);

    Optional<RefreshTokenInfo> findRefreshToken(String token);

    Optional<RefreshTokenInfo> consumeRefreshToken(String token);

    void deleteRefreshToken(String userId, String tokenId);

    void deleteAllRefreshTokens(String userId);

    void blacklistAccessToken(String jti, Duration ttl);

    boolean isAccessTokenBlacklisted(String jti);

    record RefreshTokenInfo(String userId, String tokenId, String username,
                            String familyId, int tokenVersion) {
    }
}
