package com.example.oauth.security;

import com.example.oauth.config.JwtProperties;
import com.example.oauth.config.SecurityProperties;
import com.example.oauth.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器
 * <p>
 * 继承 OncePerRequestFilter，实现：
 * 1. Token 解析
 * 2. 签名校验
 * 3. Redis 黑名单检查
 * 4. URL 黑白名单控制（Ant 风格）
 * 5. 设置 SecurityContext
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProperties jwtProperties;
    private final SecurityProperties securityProperties;
    private final TokenService tokenService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationFilter(JwtProperties jwtProperties,
                                   SecurityProperties securityProperties,
                                   TokenService tokenService) {
        this.jwtProperties = jwtProperties;
        this.securityProperties = securityProperties;
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 白名单 URL 直接放行
        String requestUri = request.getRequestURI();
        if (isWhiteListed(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 从请求头获取 Token
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 校验 Token
        SecurityUser securityUser = tokenService.validateAccessToken(token);
        if (securityUser != null) {
            // 设置认证信息到 SecurityContext
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    securityUser, null, securityUser.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("用户 [{}] 认证成功，访问 URI: {}", securityUser.getUsername(), requestUri);
        } else {
            log.warn("Token 校验失败，URI: {}", requestUri);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头解析 Token
     */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(jwtProperties.getHeader());
        if (StringUtils.hasText(header) && header.startsWith(jwtProperties.getTokenPrefix())) {
            return header.substring(jwtProperties.getTokenPrefix().length());
        }
        return null;
    }

    /**
     * 检查 URL 是否在白名单中
     */
    private boolean isWhiteListed(String uri) {
        List<String> whiteUrls = securityProperties.getIgnore().getUrls();
        if (whiteUrls == null || whiteUrls.isEmpty()) {
            return false;
        }
        return whiteUrls.stream().anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }
}
