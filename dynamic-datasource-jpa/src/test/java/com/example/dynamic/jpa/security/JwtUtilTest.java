package com.example.dynamic.jpa.security;

import com.example.dynamic.jpa.common.util.JwtUtil;
import com.example.dynamic.jpa.system.vo.LoginInfo;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {
    private static final String SECRET = "unit-test-only-secret-at-least-32-bytes";

    @Test
    void tokenContainsSignedUserAndTenantIdentity() {
        JwtUtil jwt = new JwtUtil(SECRET);
        LoginInfo info = new LoginInfo();
        info.setUserId(5);
        info.setTenantId(2);
        String token = jwt.issueToken("user@tenant", info);
        assertThat(jwt.parseToken(token).getSubject()).isEqualTo("user@tenant");
        assertThat(LoginInfo.getLoginInfoByToken("Bearer " + token, jwt))
                .extracting(LoginInfo::getUserId, LoginInfo::getTenantId).containsExactly(5, 2);
    }

    @Test
    void weakSecretIsRejectedBeforeServingRequests() {
        assertThatThrownBy(() -> new JwtUtil("short-test-secret")).isInstanceOf(WeakKeyException.class);
    }

    @Test
    void malformedAndWronglySignedTokensAreRejected() {
        JwtUtil jwt = new JwtUtil(SECRET);
        assertThatThrownBy(() -> jwt.parseToken("invalid.token" )).isInstanceOf(JwtException.class);
        String token = JwtUtil.createToken("user", "different-unit-test-secret-at-least-32-bytes");
        assertThatThrownBy(() -> jwt.parseToken(token)).isInstanceOf(JwtException.class);
    }
}
