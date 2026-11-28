package com.example.dynamic.jpa.security;

import com.example.common.result.ResultBody;
import com.example.dynamic.jpa.common.util.JSONUtils;
import com.example.dynamic.jpa.exception.ExceptionCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * 401返回 token有误  统一处理 AuthenticationException 异常
 *
 * @author zhaojh
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.write(JSONUtils.toString(ResultBody.error(ExceptionCode.TOKEN_ERROR.getCode(),
                "认证失败或登录已过期，请重新登录")));
        out.flush();
        out.close();
    }

}
