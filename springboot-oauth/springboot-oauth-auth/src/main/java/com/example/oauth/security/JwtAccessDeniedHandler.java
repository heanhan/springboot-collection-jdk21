package com.example.oauth.security;

import com.alibaba.fastjson2.JSONObject;
import com.example.oauth.common.enums.AuthErrorEnum;
import com.example.oauth.common.result.ResultBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 无权限处理器
 * <p>
 * 实现 AccessDeniedHandler 接口
 */
@Slf4j
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException e) throws IOException {
        log.warn("无权限访问: {} - {}", request.getRequestURI(), e.getMessage());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");

        ResultBody<?> result = ResultBody.error(AuthErrorEnum.ACCESS_DENIED);
        PrintWriter writer = response.getWriter();
        writer.write(JSONObject.toJSONString(result));
        writer.flush();
        writer.close();
    }
}
