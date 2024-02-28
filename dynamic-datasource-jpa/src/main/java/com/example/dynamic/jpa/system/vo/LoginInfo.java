package com.example.dynamic.jpa.system.vo;

import com.example.dynamic.jpa.common.util.JwtUtil;
import com.example.dynamic.jpa.common.util.MapUtils;
import io.jsonwebtoken.Claims;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * @Author: zhaojh
 * @ClassName: LoginInfo
 * @CreateDate: 2022-11-04 9:50
 * @Description: 登录的用户信息
 */
@Data
@Slf4j
public class LoginInfo {
    /**
     * 用户id
     */
    private Integer userId;

    /**
     * 租户的用户名
     */
    private String tenantName;

    /**
     * 租户id
     */
    private Integer tenantId = 0;

    /**
     * 从token中解析用户信息
     *
     * @param token token
     * @return cn.greenbon.api.utils.Identity
     */
    public static LoginInfo getLoginInfoByToken(String token) {
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        try {
            token = token.replace(JwtUtil.TOKEN_PREFIX, JwtUtil.EMPTY_STRING);
            Claims tokenBody = JwtUtil.getTokenBody(token, JwtUtil.TOKEN_SECRET);
            Map<String, Object> extendInfo = (Map<String, Object>) tokenBody.get(JwtUtil.EXTEND_INFO);
            return MapUtils.map2Bean(extendInfo, LoginInfo.class);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }
}
