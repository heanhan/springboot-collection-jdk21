package com.example.ddd.auth.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置。
 *
 * <p><b>关键设计决策：</b>
 * <ul>
 *   <li>{@code @EnableWebSecurity}：<b>不启用</b> {@code @EnableMethodSecurity}，
 *       因为本项目禁用 {@code @PreAuthorize / @Secured / @RolesAllowed}，
 *       改用自定义 {@link com.example.ddd.auth.infrastructure.security.permission.RequiresPermission} + AOP。</li>
 *   <li>{@code SessionCreationPolicy.STATELESS}：JWT 无状态，服务端不存 Session。</li>
 *   <li>关闭 CSRF：REST API 不使用 Cookie，无 CSRF 风险。</li>
 *   <li>关闭 formLogin / httpBasic：改用自定义 JwtAuthenticationFilter。</li>
 *   <li>{@code JwtAuthenticationFilter} 加在 {@code UsernamePasswordAuthenticationFilter} 之前。</li>
 * </ul>
 *
 * <p><b>白名单：</b>
 * 登录/注册/刷新/健康检查/OpenAPI 文档不需要 Token。</p>
 *
 * <p><b>为什么保留 PasswordEncoder Bean？</b>
 * 虽然本项目通过 {@link com.example.ddd.auth.domain.service.PasswordHasher} 抽象密码哈希，
 * 但 Spring Security 内部某些组件（例如 {@code DaoAuthenticationProvider}）
 * 仍然会寻找 PasswordEncoder Bean。暴露出来避免装配报错。</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthEntryPointImpl authEntryPoint;
    private final AccessDeniedHandlerImpl accessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          AuthEntryPointImpl authEntryPoint,
                          AccessDeniedHandlerImpl accessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authEntryPoint = authEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 白名单
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/auth/login",
                                "/auth/register",
                                "/auth/refresh",
                                "/auth/internal/**",
                                "/actuator/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/doc.html",
                                "/error"
                        ).permitAll()
                        // 其余全部需要认证
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
