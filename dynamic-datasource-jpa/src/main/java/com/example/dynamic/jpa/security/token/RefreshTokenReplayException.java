package com.example.dynamic.jpa.security.token;

public class RefreshTokenReplayException extends RuntimeException {
    public RefreshTokenReplayException() {
        super("检测到 refresh token 重放，已撤销该登录会话");
    }
}
