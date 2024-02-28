package com.example.dynamic.jpa.security;

import com.example.dynamic.jpa.exception.BaseException;
import com.example.dynamic.jpa.exception.ExceptionCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 登录验证过滤器
 * <p>
 * 该类继承自UsernamePasswordAuthenticationFilter
 * attemptAuthentication ：接收并解析用户凭证。
 *
 * @author zhaojh
 */
@Component
public class UserJwtLoginFilter extends UsernamePasswordAuthenticationFilter {

//    @Resource
//    @Override
//    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
//        super.setAuthenticationManager(authenticationManager);
//    }

    @Resource(name = "myAuthenticationFailureHandler")
    @Override
    public void setAuthenticationFailureHandler(AuthenticationFailureHandler failureHandler) {
        super.setAuthenticationFailureHandler(failureHandler);
    }

    @Resource(name="myAuthenticationSuccessHandler")
    @Override
    public void setAuthenticationSuccessHandler(AuthenticationSuccessHandler successHandler) {
        super.setAuthenticationSuccessHandler(successHandler);
    }

//    public UserJwtLoginFilter() {
//        super.setFilterProcessesUrl("/api/api/login");
//    }

    /**
     * 接收并解析用户凭证
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        try {
            //从输入流中获取到登录的信息
            LoginUser loginUser = new ObjectMapper().readValue(request.getInputStream(), LoginUser.class);

            Authentication authenticate = getAuthenticationManager().authenticate(new UsernamePasswordAuthenticationToken(loginUser.getUsername(),
                    loginUser.getPassword()));
            return authenticate;
        } catch (IOException e) {
            throw new BaseException(ExceptionCode.ARGUMENT.getCode(), "登录字段有误");
        }
    }
}