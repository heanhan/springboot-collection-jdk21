package com.example.dynamic.jpa.security;

import com.example.common.result.ResultBody;
import com.example.dynamic.jpa.common.util.JSONUtils;
import com.example.dynamic.jpa.exception.ExceptionCode;
import com.example.dynamic.jpa.exception.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
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
        if (authException instanceof JwtException) {
            //todo 需要对返回的值的异常方法做处理
            out.write(JSONUtils.toString(ResultBody.error(((JwtException) authException).getCode(), authException.getMessage())));
        } else if (authException instanceof BadCredentialsException) {
            out.write(JSONUtils.toString(ResultBody.error(ExceptionCode.UNAUTHORIZED.getCode(), authException.getMessage())));
        } else {
            out.write(JSONUtils.toString(ResultBody.error(ExceptionCode.UNAUTHORIZED.getCode(),
                    ExceptionCode.UNAUTHORIZED.getMsg())));
        }
        out.flush();
        out.close();
    }

}
