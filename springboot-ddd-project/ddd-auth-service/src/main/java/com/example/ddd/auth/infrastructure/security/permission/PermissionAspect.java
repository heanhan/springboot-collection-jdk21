package com.example.ddd.auth.infrastructure.security.permission;

import com.example.ddd.auth.infrastructure.security.LoginUserPrincipal;
import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

/**
 * 权限校验切面（配合 {@link RequiresPermission} / {@link RequiresRole}）。
 *
 * <p><b>为什么用 {@code @Around}？</b>
 * Around 通知可以在<b>方法执行前后</b>都插入逻辑；权限校验属于"前置阻断"，
 * 未通过时抛异常终止执行，通过时才 {@code pjp.proceed()} 放行原方法。</p>
 *
 * <p><b>校验流程：</b>
 * <ol>
 *   <li>从 {@code SecurityContextHolder} 取 {@link Authentication}（由 {@link com.example.ddd.auth.infrastructure.security.JwtAuthenticationFilter} 放入）。</li>
 *   <li>取 Principal 中的 {@code permissions} / {@code roles} 集合。</li>
 *   <li>与注解要求的集合做匹配（ANY / ALL）。</li>
 *   <li>不匹配抛 {@link BusinessException}(FORBIDDEN)。</li>
 * </ol>
 *
 * <p><b>为什么方法/类两级查找？</b>
 * Spring 的 {@code AnnotatedElementUtils.findMergedAnnotation} 会向上查父类/接口，
 * 支持"类级默认权限 + 方法级覆盖"的常见模式。</p>
 */
@Aspect
@Component
public class PermissionAspect {

    private static final Logger log = LoggerFactory.getLogger(PermissionAspect.class);

    @Around("@annotation(com.example.ddd.auth.infrastructure.security.permission.RequiresPermission) "
            + "|| @within(com.example.ddd.auth.infrastructure.security.permission.RequiresPermission) "
            + "|| @annotation(com.example.ddd.auth.infrastructure.security.permission.RequiresRole) "
            + "|| @within(com.example.ddd.auth.infrastructure.security.permission.RequiresRole)")
    public Object checkAccess(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();

        RequiresPermission rp = AnnotatedElementUtils.findMergedAnnotation(method, RequiresPermission.class);
        RequiresRole rr = AnnotatedElementUtils.findMergedAnnotation(method, RequiresRole.class);

        if (rp == null && rr == null) {
            return pjp.proceed();
        }

        LoginUserPrincipal principal = currentPrincipal(method);

        if (rp != null) {
            assertPermission(principal, rp, method);
        }
        if (rr != null) {
            assertRole(principal, rr, method);
        }
        return pjp.proceed();
    }

    // ============================================================
    // 内部实现
    // ============================================================

    private LoginUserPrincipal currentPrincipal(Method method) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof LoginUserPrincipal principal)) {
            log.warn("[Permission] unauthenticated access to {}", method.getName());
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或 Token 已失效");
        }
        return principal;
    }

    private void assertPermission(LoginUserPrincipal principal, RequiresPermission rp, Method method) {
        Set<String> owned = principal.permissions() == null ? Set.of() : principal.permissions();
        String[] required = rp.value();
        boolean pass = switch (rp.logical()) {
            case ANY -> Arrays.stream(required).anyMatch(owned::contains);
            case ALL -> Arrays.stream(required).allMatch(owned::contains);
        };
        if (!pass) {
            log.warn("[Permission] denied: user={} required={} owned={} method={}",
                    principal.userId(), Arrays.toString(required), owned, method.getName());
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "缺少权限: " + Arrays.toString(required));
        }
    }

    private void assertRole(LoginUserPrincipal principal, RequiresRole rr, Method method) {
        Set<String> owned = principal.roles() == null ? Set.of() : principal.roles();
        String[] required = rr.value();
        boolean pass = switch (rr.logical()) {
            case ANY -> Arrays.stream(required).anyMatch(owned::contains);
            case ALL -> Arrays.stream(required).allMatch(owned::contains);
        };
        if (!pass) {
            log.warn("[Role] denied: user={} required={} owned={} method={}",
                    principal.userId(), Arrays.toString(required), owned, method.getName());
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "缺少角色: " + Arrays.toString(required));
        }
    }
}
