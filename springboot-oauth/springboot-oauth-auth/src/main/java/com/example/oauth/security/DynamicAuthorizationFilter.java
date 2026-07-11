package com.example.oauth.security;

import com.example.oauth.config.SecurityProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 动态权限鉴权过滤器
 * <p>
 * 在 JWT 认证过滤器设置好 SecurityContext 之后、FilterSecurityInterceptor 之前执行：
 * 依据 {@link DynamicPermissionCache} 中的「URL + 方法 -> 权限编码」规则，
 * 校验当前登录用户是否具备访问所请求资源的权限编码。
 * <ul>
 *     <li>白名单/未认证请求：放行，交由后续 anyRequest().authenticated() 处理；</li>
 *     <li>未命中任何权限规则：视为不纳入接口权限管控，已认证即可访问；</li>
 *     <li>命中规则但用户不具备任一所需权限：抛出 AccessDeniedException，交给 JwtAccessDeniedHandler。</li>
 * </ul>
 */
@Slf4j
public class DynamicAuthorizationFilter extends OncePerRequestFilter {

    private final DynamicPermissionCache permissionCache;
    private final SecurityProperties securityProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public DynamicAuthorizationFilter(DynamicPermissionCache permissionCache,
                                      SecurityProperties securityProperties) {
        this.permissionCache = permissionCache;
        this.securityProperties = securityProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        // 白名单直接放行
        if (isWhiteListed(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 未认证或匿名：放行，由 anyRequest().authenticated() 返回 401
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            filterChain.doFilter(request, response);
            return;
        }

        String method = request.getMethod().toUpperCase();

        // 命中的权限规则
        List<DynamicPermissionCache.PermissionRule> matched = permissionCache.getRules().stream()
                .filter(rule -> pathMatcher.match(rule.getUrlPattern(), uri))
                .filter(rule -> rule.getMethods().isEmpty() || rule.getMethods().contains(method))
                .collect(Collectors.toList());

        // 未纳入接口权限管控，已认证即可访问
        if (matched.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        Set<String> userAuthorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        boolean allowed = matched.stream().anyMatch(rule -> userAuthorities.contains(rule.getPermCode()));
        if (!allowed) {
            log.warn("用户 [{}] 访问 [{} {}] 权限不足，所需权限之一: {}",
                    authentication.getName(), method, uri,
                    matched.stream().map(DynamicPermissionCache.PermissionRule::getPermCode).collect(Collectors.toList()));
            throw new AccessDeniedException("权限不足，无法访问该资源");
        }

        filterChain.doFilter(request, response);
    }

    private boolean isWhiteListed(String uri) {
        List<String> whiteUrls = securityProperties.getIgnore().getUrls();
        if (whiteUrls == null || whiteUrls.isEmpty()) {
            return false;
        }
        return whiteUrls.stream().anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }
}
