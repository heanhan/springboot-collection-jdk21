package com.example.oauth.security;

import com.alibaba.fastjson2.JSONObject;
import com.example.oauth.common.result.ResultBody;
import com.example.oauth.config.JwtProperties;
import com.example.oauth.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 登出成功处理器
 * <p>
 * 实现 LogoutSuccessHandler 接口
 * 登出时将 Access Token 和 Refresh Token 加入黑名单
 */
@Slf4j
@Component
public class LogoutSuccessHandlerImpl implements LogoutSuccessHandler {

    private final TokenService tokenService;
    private final JwtProperties jwtProperties;

    public LogoutSuccessHandlerImpl(TokenService tokenService, JwtProperties jwtProperties) {
        this.tokenService = tokenService;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                                Authentication authentication) throws IOException {
        // 获取 Access Token
        String accessToken = request.getHeader(jwtProperties.getHeader());
        // 获取 Refresh Token（从请求头或参数）
        String refreshToken = request.getHeader("Refresh-Token");
        if (!StringUtils.hasText(refreshToken)) {
            refreshToken = request.getParameter("refreshToken");
        }

        // 加入黑名单
        if (StringUtils.hasText(accessToken) || StringUtils.hasText(refreshToken)) {
            tokenService.blacklistTokens(accessToken, refreshToken);
            log.info("用户登出成功，Token 已加入黑名单");
        }

        response.setContentType("application/json;charset=UTF-8");
        ResultBody<?> result = ResultBody.success("登出成功");
        PrintWriter writer = response.getWriter();
        writer.write(JSONObject.toJSONString(result));
        writer.flush();
        writer.close();
    }
}
