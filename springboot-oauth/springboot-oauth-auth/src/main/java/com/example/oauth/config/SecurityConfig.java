package com.example.oauth.config;

import com.example.oauth.security.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 核心配置
 * <p>
 * 继承 WebSecurityConfigurerAdapter，通过重写 configure 方法完成 Security 配置。
 * 不使用 @PreAuthorize 等注解方式。
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = false, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final LoginSuccessHandler loginSuccessHandler;
    private final LoginFailureHandler loginFailureHandler;
    private final LogoutSuccessHandlerImpl logoutSuccessHandler;
    private final JwtProperties jwtProperties;
    private final SecurityProperties securityProperties;
    private final TokenServiceBeanHolder tokenServiceBeanHolder;
    private final DynamicPermissionCache dynamicPermissionCache;

    /**
     * 内部辅助类，用于延迟注入 TokenService（避免循环依赖）
     */
    @org.springframework.stereotype.Component
    public static class TokenServiceBeanHolder {
        private final com.example.oauth.service.TokenService tokenService;

        public TokenServiceBeanHolder(com.example.oauth.service.TokenService tokenService) {
            this.tokenService = tokenService;
        }

        public com.example.oauth.service.TokenService getTokenService() {
            return tokenService;
        }
    }

    public SecurityConfig(UserDetailsService userDetailsService,
                          JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                          JwtAccessDeniedHandler jwtAccessDeniedHandler,
                          LoginSuccessHandler loginSuccessHandler,
                          LoginFailureHandler loginFailureHandler,
                          LogoutSuccessHandlerImpl logoutSuccessHandler,
                          JwtProperties jwtProperties,
                          SecurityProperties securityProperties,
                          TokenServiceBeanHolder tokenServiceBeanHolder,
                          DynamicPermissionCache dynamicPermissionCache) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
        this.loginSuccessHandler = loginSuccessHandler;
        this.loginFailureHandler = loginFailureHandler;
        this.logoutSuccessHandler = logoutSuccessHandler;
        this.jwtProperties = jwtProperties;
        this.securityProperties = securityProperties;
        this.tokenServiceBeanHolder = tokenServiceBeanHolder;
        this.dynamicPermissionCache = dynamicPermissionCache;
    }

    // ==================== 密码编码器 ====================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ==================== AuthenticationManager ====================

    /**
     * 暴露 AuthenticationManager 为 Bean，供 AuthController 登录认证使用
     */
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    // ==================== JwtAuthenticationFilter ====================

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtProperties, securityProperties, tokenServiceBeanHolder.getTokenService());
    }

    // ==================== DynamicAuthorizationFilter ====================

    @Bean
    public DynamicAuthorizationFilter dynamicAuthorizationFilter() {
        return new DynamicAuthorizationFilter(dynamicPermissionCache, securityProperties);
    }

    // ==================== configure(AuthenticationManagerBuilder) ====================

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    // ==================== configure(WebSecurity) ====================

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring()
                .antMatchers(
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/v2/api-docs",
                        "/v3/api-docs",
                        "/webjars/**",
                        "/favicon.ico",
                        "/error"
                );
    }

    // ==================== configure(HttpSecurity) ====================

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（前后端分离不需要）
                .csrf().disable()
                // CORS 配置
                .cors().and()
                // 基于 Token，不需要 Session
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                // 授权配置
                .authorizeRequests()
                // 白名单 URL
                .antMatchers(securityProperties.getIgnore().getUrls().toArray(new String[0])).permitAll()
                // 其他所有请求需要认证
                .anyRequest().authenticated()
                .and()
                // 异常处理
                .exceptionHandling()
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler)
                .and()
                // 登录配置（表单方式，处理 application/x-www-form-urlencoded 请求）
                // JSON 方式的登录由 AuthController.login() 处理
                .formLogin()
                .loginProcessingUrl("/auth/form-login")
                .successHandler(loginSuccessHandler)
                .failureHandler(loginFailureHandler)
                .and()
                // 登出配置
                .logout()
                .logoutUrl("/auth/logout")
                .logoutSuccessHandler(logoutSuccessHandler)
                // HTTPS 支持：生产环境取消下方注释启用强制 HTTPS
                // .and().requiresChannel().anyRequest().requiresSecure()
                .and()
                // 添加 JWT 认证过滤器
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                // 添加动态权限鉴权过滤器（位于 ExceptionTranslationFilter 之后，以便 AccessDeniedException 被捕获）
                .addFilterBefore(dynamicAuthorizationFilter(), FilterSecurityInterceptor.class);
    }
}
