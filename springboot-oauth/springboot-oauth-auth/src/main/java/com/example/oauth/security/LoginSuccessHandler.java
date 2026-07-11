package com.example.oauth.security;

import com.alibaba.fastjson2.JSONObject;
import com.example.oauth.common.result.ResultBody;
import com.example.oauth.service.LoginAttemptService;
import com.example.oauth.service.TokenService;
import com.example.oauth.vo.TokenVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 登录成功处理器
 * <p>
 * 实现 AuthenticationSuccessHandler 接口
 * 登录成功后生成双 Token（Access + Refresh）并返回
 */
@Slf4j
@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final TokenService tokenService;
    private final LoginAttemptService loginAttemptService;

    public LoginSuccessHandler(TokenService tokenService, LoginAttemptService loginAttemptService) {
        this.tokenService = tokenService;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();

        // 登录成功，重置失败计数
        loginAttemptService.resetAttempts(securityUser.getUsername());

        // 生成双 Token
        TokenVO tokenVO = tokenService.generateDualToken(securityUser);

        log.info("用户 [{}] 登录成功", securityUser.getUsername());

        response.setContentType("application/json;charset=UTF-8");
        ResultBody<TokenVO> result = ResultBody.success(tokenVO);
        PrintWriter writer = response.getWriter();
        writer.write(JSONObject.toJSONString(result));
        writer.flush();
        writer.close();
    }
}
