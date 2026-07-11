package com.example.oauth.security;

import com.alibaba.fastjson2.JSONObject;
import com.example.oauth.common.enums.AuthErrorEnum;
import com.example.oauth.common.result.ResultBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 未登录 / Token 无效处理器
 * <p>
 * 实现 AuthenticationEntryPoint 接口
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException e) throws IOException {
        log.warn("未登录或 Token 无效: {} - {}", request.getRequestURI(), e.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        ResultBody<?> result = ResultBody.error(AuthErrorEnum.NOT_LOGGED_IN);
        PrintWriter writer = response.getWriter();
        writer.write(JSONObject.toJSONString(result));
        writer.flush();
        writer.close();
    }
}
