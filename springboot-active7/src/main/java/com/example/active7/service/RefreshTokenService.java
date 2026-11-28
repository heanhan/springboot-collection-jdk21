package com.example.active7.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.active7.mapper.SysRefreshTokenMapper;
import com.example.active7.model.SysRefreshToken;
import com.example.active7.security.Active7JwtProperties;
import com.example.common.exceptins.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final SysRefreshTokenMapper refreshTokenMapper;
    private final Active7JwtProperties properties;

    public RefreshTokenService(SysRefreshTokenMapper refreshTokenMapper,
                               Active7JwtProperties properties) {
        this.refreshTokenMapper = refreshTokenMapper;
        this.properties = properties;
    }

    @Transactional
    public String create(String userId) {
        String rawToken = newToken();
        SysRefreshToken token = new SysRefreshToken();
        token.setTokenId(sha256(rawToken));
        token.setUserId(userId);
        token.setExpiresAt(LocalDateTime.now().plusSeconds(properties.getRefreshTokenSeconds()));
        refreshTokenMapper.insert(token);
        return rawToken;
    }

    @Transactional(noRollbackFor = RefreshTokenSecurityException.class)
    public String rotate(String rawToken) {
        if (rawToken == null || rawToken.trim().isEmpty()) {
            throw new BizException(401, "Refresh Token 不能为空");
        }
        String tokenId = sha256(rawToken);
        SysRefreshToken current = refreshTokenMapper.selectById(tokenId);
        if (current == null) {
            throw new BizException(401, "Refresh Token 不存在，请重新登录");
        }
        if (current.getRevokedAt() != null) {
            revokeAllActive(current.getUserId());
            throw new RefreshTokenSecurityException("Refresh Token 已被使用或撤销，请重新登录");
        }
        LocalDateTime now = LocalDateTime.now();
        if (!current.getExpiresAt().isAfter(now)) {
            throw new BizException(401, "Refresh Token 已过期，请重新登录");
        }

        int claimed = refreshTokenMapper.update(null, new LambdaUpdateWrapper<SysRefreshToken>()
                .eq(SysRefreshToken::getTokenId, tokenId)
                .isNull(SysRefreshToken::getRevokedAt)
                .gt(SysRefreshToken::getExpiresAt, now)
                .set(SysRefreshToken::getRevokedAt, now));
        if (claimed == 0) {
            revokeAllActive(current.getUserId());
            throw new RefreshTokenSecurityException("Refresh Token 重复使用，请重新登录");
        }
        create(current.getUserId());
        return current.getUserId();
    }

    public static class RefreshTokenSecurityException extends BizException {

        public RefreshTokenSecurityException(String message) {
            super(401, message);
        }
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.trim().isEmpty()) {
            return;
        }
        refreshTokenMapper.update(null, new LambdaUpdateWrapper<SysRefreshToken>()
                .eq(SysRefreshToken::getTokenId, sha256(rawToken))
                .isNull(SysRefreshToken::getRevokedAt)
                .set(SysRefreshToken::getRevokedAt, LocalDateTime.now()));
    }

    @Transactional
    public void revokeAllForUser(String userId) {
        revokeAllActive(userId);
    }

    private void revokeAllActive(String userId) {
        refreshTokenMapper.update(null, new LambdaUpdateWrapper<SysRefreshToken>()
                .eq(SysRefreshToken::getUserId, userId)
                .isNull(SysRefreshToken::getRevokedAt)
                .set(SysRefreshToken::getRevokedAt, LocalDateTime.now()));
    }

    private String newToken() {
        byte[] random = new byte[32];
        SECURE_RANDOM.nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            char[] chars = new char[digest.length * 2];
            for (int i = 0; i < digest.length; i++) {
                int valueByte = digest[i] & 0xff;
                chars[i * 2] = HEX[valueByte >>> 4];
                chars[i * 2 + 1] = HEX[valueByte & 0x0f];
            }
            return new String(chars);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", exception);
        }
    }
}
