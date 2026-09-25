package com.example.dynamic.jpa.security;

import com.example.dynamic.jpa.system.entity.AuthNode;
import com.example.dynamic.jpa.system.service.AuthNodeService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 基于权限节点的动态 RBAC 授权管理器
 *
 * @author zhaojh
 */
@Component
public class RbacAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    @Resource
    private AuthNodeService authNodeService;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        if (auth == null || !auth.isAuthenticated()
                || auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            throw new org.springframework.security.authentication.InsufficientAuthenticationException("未登录");
        }
        String path = resolvePath(context.getRequest().getRequestURI(), context.getRequest().getContextPath());
        String method = context.getRequest().getMethod();
        List<AuthNode> nodes = authNodeService.listActiveAuthNodes();
        List<AuthNode> matched = nodes.stream()
                .filter(node -> pathMatcher.match(node.getPath(), path))
                .filter(node -> StringUtils.isBlank(node.getMethod())
                        || node.getMethod().equalsIgnoreCase(method))
                .toList();
        if (matched.isEmpty()) {
            return new AuthorizationDecision(false);
        }
        Set<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        for (AuthNode node : matched) {
            List<String> allowedRoles = authNodeService.listRolesForNode(node);
            if (allowedRoles.isEmpty()) {
                return new AuthorizationDecision(false);
            }
            if (authorities.stream().anyMatch(allowedRoles::contains)) {
                return new AuthorizationDecision(true);
            }
        }
        return new AuthorizationDecision(false);
    }

    private String resolvePath(String requestUri, String contextPath) {
        if (StringUtils.isBlank(contextPath) || !requestUri.startsWith(contextPath)) {
            return requestUri;
        }
        String path = requestUri.substring(contextPath.length());
        return StringUtils.isBlank(path) ? "/" : path;
    }
}
