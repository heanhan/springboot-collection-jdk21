package com.example.oauth.security;

import com.alibaba.fastjson2.JSONObject;
import com.example.oauth.common.enums.AuthErrorEnum;
import com.example.oauth.common.result.ResultBody;
import com.example.oauth.service.LoginAttemptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 登录失败处理器
 * <p>
 * 实现 AuthenticationFailureHandler 接口
 * 记录失败次数，返回错误信息
 */
@Slf4j
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final LoginAttemptService loginAttemptService;

    public LoginFailureHandler(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        // 尝试从请求参数中获取用户名
        String username = request.getParameter("username");
        if (username == null) {
            username = "unknown";
        }

        // 记录失败
        loginAttemptService.recordFailedAttempt(username);
        int remaining = loginAttemptService.getRemainingAttempts(username);

        log.warn("用户 [{}] 登录失败，剩余尝试次数: {}", username, remaining);

        response.setContentType("application/json;charset=UTF-8");

        ResultBody<?> result;
        if (loginAttemptService.isLocked(username)) {
            long lockMinutes = loginAttemptService.getLockTimeRemaining(username);
            result = ResultBody.error(AuthErrorEnum.LOGIN_FAIL_LIMIT.getResultCode(),
                    "账号已被锁定，请 " + lockMinutes + " 分钟后重试");
        } else {
            result = ResultBody.error(AuthErrorEnum.TOKEN_INVALID.getResultCode(),
                    "用户名或密码错误，剩余尝试次数: " + remaining);
        }

        PrintWriter writer = response.getWriter();
        writer.write(JSONObject.toJSONString(result));
        writer.flush();
        writer.close();
    }
}
