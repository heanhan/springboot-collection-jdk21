package com.example.dynamic.jpa.security;

import com.example.dynamic.jpa.common.util.JwtUtil;
import com.example.dynamic.jpa.exception.ExceptionCode;
import com.example.dynamic.jpa.exception.JwtException;
import com.example.dynamic.jpa.security.token.TokenService;
import com.example.dynamic.jpa.system.config.LoginInfoHolder;
import com.example.dynamic.jpa.system.vo.LoginInfo;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * token的校验
 * OncePerRequestFilter， 确保在一次请求只通过一次filter，而不需要重复执行，
 * 从http头的Authorization 项读取token数据，然后用Jwts包提供的方法校验token的合法性。
 * 如果校验通过，就认为这是一个取得授权的合法请求
 *
 * @author zhaojh
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Resource(name = "authUserServiceImpl")
    private UserDetailsService userDetailsService;

    @Resource
    private AuthenticationEntryPoint authenticationEntryPoint;

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws IOException, ServletException {
        LoginInfoHolder.clear();
        try {
            String header = request.getHeader(JwtUtil.TOKEN_HEADER);
            if (StringUtils.isNotBlank(header)) {
                try {
                    if (!header.startsWith(JwtUtil.TOKEN_PREFIX)) {
                        throw new JwtException("token格式错误");
                    }
                    SecurityContextHolder.getContext().setAuthentication(getAuthentication(header));
                } catch (org.springframework.security.core.AuthenticationException e) {
                    SecurityContextHolder.clearContext();
                    authenticationEntryPoint.commence(request, response, e);
                    return;
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            LoginInfoHolder.clear();
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 这里从token中获取用户信息
     */
    private UsernamePasswordAuthenticationToken getAuthentication(String tokenHeader) {
        String token = tokenHeader.substring(JwtUtil.TOKEN_PREFIX.length());
        try {
            io.jsonwebtoken.Claims claims = jwtUtil.parseToken(token);
            if (StringUtils.isBlank(claims.getSubject()) || claims.getExpiration() == null) {
                throw new JwtException("token缺少必要信息");
            }
            // 登出黑名单校验：已被拉黑的 access token 视为无效
            if (tokenService.isBlacklisted(claims.getId())) {
                throw new JwtException(ExceptionCode.TOKEN_ERROR.getCode(), ExceptionCode.TOKEN_ERROR.getMsg());
            }
            UserDetails details = userDetailsService.loadUserByUsername(claims.getSubject());
            new org.springframework.security.authentication.AccountStatusUserDetailsChecker().check(details);
            if (!(details instanceof JwtUser jwtUser)
                    || !(claims.get(JwtUtil.EXTEND_INFO) instanceof java.util.Map<?, ?> info)
                    || !(info.get("userId") instanceof Number userId)
                    || !(info.get("tenantId") instanceof Number tenantId)
                    || !java.util.Objects.equals(userId.intValue(), jwtUser.getUser().getId())
                    || !java.util.Objects.equals(tenantId.intValue(), jwtUser.getUser().getTenantId())) {
                throw new JwtException("token身份与当前用户不匹配");
            }
            return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        } catch (ExpiredJwtException e) {
            throw new JwtException(ExceptionCode.TOKEN_EXPIRE.getCode(), ExceptionCode.TOKEN_EXPIRE.getMsg());
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            throw new JwtException(ExceptionCode.TOKEN_ERROR.getCode(), ExceptionCode.TOKEN_ERROR.getMsg());
        }
    }

}