package com.example.ddd.auth.infrastructure.security;

import com.example.ddd.auth.application.port.TokenProvider;
import com.example.ddd.auth.application.port.TokenStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT 认证过滤器（Spring Security Filter 链中的自定义 Filter）。
 *
 * <p><b>执行流程：</b>
 * <ol>
 *   <li>从 Header {@code Authorization: Bearer xxx} 提取 Token。</li>
 *   <li>调用 {@link TokenProvider#parseAccessToken(String)} 验签 + 解析 payload。</li>
 *   <li>调用 {@link TokenStore#isAccessTokenBlacklisted(String)} 检查是否已注销。</li>
 *   <li>构造 {@link LoginUserPrincipal} 与 {@link UsernamePasswordAuthenticationToken}，放入 SecurityContext。</li>
 *   <li>放行到下一个 Filter。</li>
 * </ol>
 *
 * <p><b>为什么继承 OncePerRequestFilter？</b>
 * 保证同一次请求只执行一次，即使被 forward/include 也不重复。</p>
 *
 * <p><b>为什么不用 Spring Security 的 OAuth2 Resource Server？</b>
 * 学习项目为了展示 Filter 的完整工作机制，手写 Filter 更透明。
 * 生产项目可以直接使用 {@code spring-boot-starter-oauth2-resource-server}。</p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final TokenProvider tokenProvider;
    private final TokenStore tokenStore;

    public JwtAuthenticationFilter(TokenProvider tokenProvider, TokenStore tokenStore) {
        this.tokenProvider = tokenProvider;
        this.tokenStore = tokenStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            String token = header.substring(PREFIX.length());
            TokenProvider.AccessTokenPayload payload = tokenProvider.parseAccessToken(token);
            if (payload != null && !tokenStore.isAccessTokenBlacklisted(payload.tokenId())) {
                LoginUserPrincipal principal = new LoginUserPrincipal(
                        payload.userId(), payload.username(), payload.tokenId(),
                        payload.roles(), payload.permissions());

                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                payload.roles().forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
                payload.permissions().forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else if (payload != null) {
                log.debug("[JWT] token in blacklist, tokenId={}", payload.tokenId());
            }
        }
        filterChain.doFilter(request, response);
    }
}
