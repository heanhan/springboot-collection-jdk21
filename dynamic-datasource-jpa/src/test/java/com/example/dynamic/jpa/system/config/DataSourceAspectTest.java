package com.example.dynamic.jpa.system.config;

import com.example.dynamic.jpa.security.JwtUser;
import com.example.dynamic.jpa.system.controller.SystemController;
import com.example.dynamic.jpa.system.entity.User;
import com.example.dynamic.jpa.system.vo.LoginInfo;
import com.example.dynamic.jpa.tenant.controller.TestController;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataSourceAspectTest {
    private final DataSourceAspect aspect = new DataSourceAspect();

    @AfterEach
    void clear() {
        LoginInfoHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void systemAnnotationOverridesAuthenticatedTenantAndRestoresPreviousContext() throws Throwable {
        authenticate(7);
        LoginInfo previous = new LoginInfo();
        previous.setTenantId(9);
        LoginInfoHolder.setTenant(previous);
        ProceedingJoinPoint point = point(new SystemController(), "login",
                com.example.dynamic.jpa.security.LoginUser.class);
        when(point.proceed()).thenAnswer(invocation -> {
            assertThat(LoginInfoHolder.getTenant().getTenantId()).isZero();
            return "ok";
        });
        assertThat(aspect.around(point)).isEqualTo("ok");
        assertThat(LoginInfoHolder.getTenant()).isSameAs(previous);
    }

    @Test
    void tenantAnnotationUsesAuthenticatedIdentityAndClearsContextAfterFailure() throws Throwable {
        authenticate(7);
        ProceedingJoinPoint point = point(new TestController(), "getTestById", Integer.class, Integer.class);
        when(point.proceed()).thenAnswer(invocation -> {
            assertThat(LoginInfoHolder.getTenant().getTenantId()).isEqualTo(7);
            throw new IllegalStateException("测试业务异常");
        });
        assertThatThrownBy(() -> aspect.around(point)).isInstanceOf(IllegalStateException.class);
        assertThat(LoginInfoHolder.getTenant()).isNull();
    }

    @Test
    void tenantAnnotationRejectsMissingIdentity() throws Throwable {
        ProceedingJoinPoint point = point(new TestController(), "getTestById", Integer.class, Integer.class);
        assertThatThrownBy(() -> aspect.around(point)).isInstanceOf(AccessDeniedException.class);
        verify(point, never()).proceed();
        assertThat(LoginInfoHolder.getTenant()).isNull();
    }

    private void authenticate(int tenantId) {
        User user = new User();
        user.setId(1);
        user.setTenantId(tenantId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new JwtUser(user, List.of()), null, List.of()));
    }

    private ProceedingJoinPoint point(Object target, String method, Class<?>... parameters) throws Exception {
        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(point.getTarget()).thenReturn(target);
        when(point.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(target.getClass().getMethod(method, parameters));
        return point;
    }
}
