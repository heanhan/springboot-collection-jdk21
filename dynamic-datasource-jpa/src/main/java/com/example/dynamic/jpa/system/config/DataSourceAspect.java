package com.example.dynamic.jpa.system.config;


import com.example.dynamic.jpa.common.util.JwtUtil;
import com.example.dynamic.jpa.system.entity.DataSourceType;
import com.example.dynamic.jpa.system.vo.LoginInfo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * @Author: zhaojh
 * @ClassName: DataSourceAspect
 * @Description: 动态切换数据源切面
 */
@Slf4j
@Aspect
@Order(1)
@Component
public class DataSourceAspect {
    //com.cncy.newuembackend.system.controller
    @Pointcut("execution (* com.example.dynamic.jpa..controller.*.*(..))")
    public void dataPointCut() {

    }

    @org.aspectj.lang.annotation.Around("dataPointCut()")
    public Object around(org.aspectj.lang.ProceedingJoinPoint joinPoint) throws Throwable {
        LoginInfo previous = LoginInfoHolder.getTenant();
        try {
            var authentication = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            LoginInfo loginInfo = authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof com.example.dynamic.jpa.security.JwtUser user
                    ? LoginInfo.fromUser(user.getUser()) : new LoginInfo();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = org.springframework.aop.support.AopUtils.getMostSpecificMethod(
                    signature.getMethod(), joinPoint.getTarget().getClass());
            MyDataSource annotation = org.springframework.core.annotation.AnnotatedElementUtils
                    .findMergedAnnotation(method, MyDataSource.class);
            if (annotation == null) {
                annotation = org.springframework.core.annotation.AnnotatedElementUtils
                        .findMergedAnnotation(joinPoint.getTarget().getClass(), MyDataSource.class);
            }
            if (annotation != null && annotation.type() == DataSourceType.SYSTEM) {
                loginInfo.setTenantId(0);
            } else if (annotation != null && annotation.type() == DataSourceType.TENANT) {
                Integer tenantId = loginInfo.getTenantId();
                if (tenantId == null || tenantId <= 0
                        || (annotation.value() > 0 && annotation.value() != tenantId)) {
                    throw new org.springframework.security.access.AccessDeniedException("无权访问该租户数据源");
                }
            }
            LoginInfoHolder.setTenant(loginInfo);
            return joinPoint.proceed();
        } finally {
            if (previous == null) {
                LoginInfoHolder.clear();
            } else {
                LoginInfoHolder.setTenant(previous);
            }
        }
    }
}
