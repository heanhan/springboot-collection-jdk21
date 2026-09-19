package com.example.ddd.auth.infrastructure.security;

import com.example.ddd.common.exception.ErrorCode;
import com.example.ddd.common.result.Result;
import com.example.ddd.common.util.JsonUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 未认证 (401) 处理器。
 *
 * <p>当 SecurityContext 中没有 Authentication 却访问受保护资源时触发。
 * 返回统一 {@link Result} JSON 而不是 Spring Security 默认的 HTML 登录页。</p>
 */
@Component
public class AuthEntryPointImpl implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.UNAUTHORIZED,
                "未认证或 Token 无效: " + authException.getMessage());
    }

    static void writeJson(HttpServletResponse response, int status, ErrorCode code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(JsonUtils.toJson(Result.fail(code, message)));
    }
}
