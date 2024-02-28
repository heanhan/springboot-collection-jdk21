package com.example.dynamic.jpa.security;

import com.example.dynamic.jpa.common.util.JSONUtils;
import com.example.dynamic.jpa.exception.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.commons.result.ResultBody;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @Author : zhaojh
 * @Description : 登录认证验证失败的处理
 */
@Component
public class MyAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response, AuthenticationException exception) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        ResultBody resultBody=null;
        if (exception instanceof BadCredentialsException || exception instanceof UsernameNotFoundException) {
            resultBody = resultBody.error("账户名或者密码输入错误!");
        } else if (exception instanceof LockedException) {
            resultBody = resultBody.error("账户被锁定，请联系管理员!");
        } else if (exception instanceof CredentialsExpiredException) {
            resultBody = resultBody.error("密码过期，请联系管理员!");
        } else if (exception instanceof AccountExpiredException) {
            resultBody = resultBody.error("账户过期，请联系管理员!");
        } else if (exception instanceof DisabledException) {
            resultBody = resultBody.error("账户被禁用，请联系管理员!");
        } else if (exception instanceof JwtException) {
            resultBody = resultBody.error(exception.getMessage());
        } else {
            resultBody = resultBody.error("登录失败!");
        }
        response.getWriter().write(JSONUtils.toString(resultBody));
    }
}