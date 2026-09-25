package com.example.dynamic.jpa.security;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


/**
 * @author zhaojh
 * spring-security权限管理的核心配置
 */
@EnableWebSecurity
@Configuration
public class SecurityConfig {

    /**
     * 动态RBAC授权管理器
     */
    @Resource
    private RbacAuthorizationManager rbacAuthorizationManager;

    /**
     * 自定义错误(401)返回数据
     */
    @Resource
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * 权限不足403处理器
     */
    @Resource
    private MyAccessDeniedHandler myAccessDeniedHandler;

    @Resource
    private AuthenticationConfiguration authenticationConfiguration;

    @Resource
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 获取AuthenticationManager（认证管理器），登录时认证使用
     * @return
     * @throws Exception
     */
    @Bean
    public AuthenticationManager authenticationManager() throws Exception{
        AuthenticationManager authenticationManager = authenticationConfiguration.getAuthenticationManager();
        return authenticationManager;
    }


    @Bean
    org.springframework.boot.web.servlet.FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
            JwtAuthenticationFilter filter) {
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * 装载BCrypt密码编码器
     */
    @Bean
    public static BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置不拦截的路径
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return new WebSecurityCustomizer() {
            @Override
            public void customize(WebSecurity web) {
//                web.ignoring().antMatchers("/doc.html")
//                        .antMatchers("/swagger-ui.html")
//                        .antMatchers("/resources/**")
//                        .antMatchers("/webjars/**")
//                        .antMatchers("/v3/api-docs/**")
//                        //ios客户端的请求路径需要放开
//                        .antMatchers("/iosapp/**")
//                        .antMatchers("/swagger-resources/**")
                ;
            }
        };
    }

    /**
     * HttpSecurity包含了原数据（主要是url）
     * 登录和错误页固定放行，其余请求由权限节点动态决定
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .csrf(org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer::disable)
                .formLogin(org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer::disable)
                .httpBasic(org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer::disable)
                .logout(org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer::disable)
                .requestCache(org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(myAccessDeniedHandler))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/login").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/refresh").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/logout").authenticated()
                        // Actuator：健康检查/信息/指标/Prometheus 抓取端点放行（生产建议置于内网或经反向代理鉴权保护）
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/metrics", "/actuator/metrics/**", "/actuator/prometheus").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().access(rbacAuthorizationManager))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return httpSecurity.build();
    }
}
