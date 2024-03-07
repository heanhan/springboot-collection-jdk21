package com.example.dynamic.jpa.security;


import com.example.common.result.ResultBody;
import com.example.dynamic.jpa.common.util.JSONUtils;
import com.example.dynamic.jpa.common.util.JwtUtil;
import com.example.dynamic.jpa.system.entity.User;
import com.example.dynamic.jpa.system.vo.LoginInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author : zhaojh
 * @Description : 登录认证成功的处理
 */
@Component
public class MyAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication auth) throws IOException {
        JwtUser jwtUser = (JwtUser) auth.getPrincipal();
        User user = jwtUser.getUser();
        LoginInfo loginInfo = new LoginInfo();
//        loginInfo.setUserId(user.getId());
//        loginInfo.setTenantId(user.getTenantId());
        String token = JwtUtil.createToken(jwtUser.getUsername(), JwtUtil.TOKEN_SECRET, loginInfo);
        Map<String, Object> map = new HashMap<>(7);
        map.put("userName", user.getUsername());
        map.put("realName", user.getNickname());
        map.put("type", user.getType());
//        map.put("id", user.getId());
        //添加租户标识
        map.put("tenantId", user.getTenantId());
        map.put(JwtUtil.TOKEN_HEADER, JwtUtil.TOKEN_PREFIX + token);
        response.setContentType("application/json; charset=utf-8");
        response.getWriter().write(JSONUtils.toString(ResultBody.success(map)));
    }
}