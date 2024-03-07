package com.example.dynamic.jpa.security;

import com.example.common.result.ResultBody;
import com.example.dynamic.jpa.common.util.JSONUtils;
import com.example.dynamic.jpa.exception.ExceptionCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * @Author : zhaojh
 * @Description : 权限不足403处理器 统一处理 AccessDeniedException 异常
 */
@Component
public class MyAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest httpServletRequest, HttpServletResponse resp,
                       AccessDeniedException e) throws IOException {
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        out.write(JSONUtils.toString(ResultBody.error(ExceptionCode.UNAUTHORIZED.getCode(),
                ExceptionCode.UNAUTHORIZED.getMsg())));
        out.flush();
        out.close();
    }
}
