package com.example.ddd.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 认证鉴权服务启动类。
 *
 * <p><b>限界上下文：Authentication &amp; Authorization</b></p>
 *
 * <p><b>技术要点：</b>
 * <ul>
 *   <li>Spring Security 6：Filter 链、PasswordEncoder、SecurityContext（<b>不使用</b>方法级注解）</li>
 *   <li>JJWT 0.12：双 Token（AccessToken + RefreshToken）签发与校验</li>
 *   <li>Redis：Token 黑名单、权限缓存、登录失败计数</li>
 *   <li>JPA + MySQL：UserCredential 聚合根持久化</li>
 *   <li>RocketMQ：发布 UserRegisteredEvent</li>
 *   <li>OpenFeign：调用 user-service 拉取角色权限</li>
 * </ul>
 *
 * <p><b>关键注解说明：</b>
 * <ul>
 *   <li>{@code @EnableFeignClients}：扫描 {@code com.example.ddd.contract} 包下的 Feign 接口</li>
 *   <li>{@code @EnableScheduling}：启用定时任务（例如清理过期 LoginSession）</li>
 *   <li>{@code @EnableAsync}：启用异步方法（事件发布可异步）</li>
 * </ul>
 *
 * @author ddd-learning
 */
@SpringBootApplication(scanBasePackages = {
        "com.example.ddd.auth",
        "com.example.ddd.common"
})
@EnableFeignClients(basePackages = "com.example.ddd.contract")
@EnableScheduling
@EnableAsync
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
