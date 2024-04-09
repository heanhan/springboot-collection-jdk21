package com.example.oauth2.leaning.config;


import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import javax.sql.DataSource;

@EnableWebSecurity
@Configuration(proxyBeanMethods = false)
public class ServerSecurityConfig {

    @Resource
    private DataSource dataSource;

    /**
     * Spring Security 的过滤器链，用于 Spring Security 的身份认证
     */
    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        // 配置放行的请求,spring6.0之后放弃了 antMatchers()方法，使用requestMatchers()
                        .requestMatchers("/api/**", "/login").permitAll()
                        // 其他任何请求都需要认证
                        .anyRequest().authenticated()
                )
                // 设置登录表单页面
                .formLogin(formLoginConfigurer -> formLoginConfigurer.loginPage("/login"));

        return http.build();
    }

    @Bean
    UserDetailsManager userDetailsManager() {
        return new JdbcUserDetailsManager(dataSource);
    }


}
