package com.example.ddd.auth.infrastructure.security;

import com.example.ddd.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 已认证但权限不足 (403) 处理器。
 */
@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        AuthEntryPointImpl.writeJson(response, HttpServletResponse.SC_FORBIDDEN,
                ErrorCode.FORBIDDEN, "权限不足: " + accessDeniedException.getMessage());
    }
}
