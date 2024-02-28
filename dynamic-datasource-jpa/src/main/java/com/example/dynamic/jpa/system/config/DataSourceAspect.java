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

    @Before("dataPointCut()")
    public void before(JoinPoint joinPoint) {
        //获取请求对象
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return;
        }
        HttpServletRequest request = requestAttributes.getRequest();
        // 从token中解析用户信息
        String token = request.getHeader(JwtUtil.TOKEN_HEADER);
        LoginInfo loginInfo = LoginInfo.getLoginInfoByToken(token);
        if (loginInfo == null) {
            loginInfo = new LoginInfo();
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        MyDataSource myDataSource = null;
        //优先判断方法上的注解
        if (method.isAnnotationPresent(MyDataSource.class)) {
            myDataSource = method.getAnnotation(MyDataSource.class);
        } else if (method.getDeclaringClass().isAnnotationPresent(MyDataSource.class)) {
            //其次判断类上的注解
            myDataSource = method.getDeclaringClass().getAnnotation(MyDataSource.class);
        }
        if (myDataSource != null) {
            DataSourceType dataSourceType = myDataSource.type();
            log.info("this is datasource: " + dataSourceType);
            if (dataSourceType.equals(DataSourceType.TENANT)) {
                loginInfo.setTenantId(myDataSource.value());
            }
        }
        LoginInfoHolder.setTenant(loginInfo);

    }

    @After("dataPointCut()")
    public void after(JoinPoint joinPoint) {
        LoginInfoHolder.clear();
    }
}
