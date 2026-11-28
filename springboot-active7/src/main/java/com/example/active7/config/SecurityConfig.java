package com.example.active7.config;

import com.example.active7.security.Active7JwtProperties;
import com.example.active7.security.DemoUserDetailsService;
import com.example.active7.security.JwtAuthenticationFilter;
import com.example.active7.security.JwtTokenProvider;
import com.example.active7.security.RbacPermissions;
import com.example.active7.security.RestAccessDeniedHandler;
import com.example.active7.security.RestAuthenticationEntryPoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(Active7JwtProperties.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(DemoUserDetailsService userDetailsService,
                                                            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   DemoUserDetailsService userDetailsService,
                                                   JwtTokenProvider jwtTokenProvider,
                                                   RestAuthenticationEntryPoint authenticationEntryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(
                jwtTokenProvider, userDetailsService, authenticationEntryPoint);

        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/login", "/index", "/css/**", "/js/**", "/images/**",
                                "/webjars/**", "/doc.html", "/swagger-ui/**", "/v3/api-docs/**",
                                "/actuator/health", "/error", "/favicon.ico")
                        .permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/refresh", "/api/auth/logout")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me")
                        .hasAuthority(RbacPermissions.AUTH_VIEW)
                        .requestMatchers(HttpMethod.POST, "/api/leave/requests")
                        .hasAuthority(RbacPermissions.LEAVE_CREATE)
                        .requestMatchers(HttpMethod.GET, "/api/leave/requests")
                        .hasAuthority(RbacPermissions.LEAVE_VIEW_ALL)
                        .requestMatchers(HttpMethod.GET, "/api/leave/requests/**")
                        .hasAnyAuthority(RbacPermissions.LEAVE_VIEW_MINE, RbacPermissions.LEAVE_VIEW_ALL)
                        .requestMatchers(HttpMethod.POST, "/api/leave/requests/{requestId}/withdraw")
                        .hasAuthority(RbacPermissions.LEAVE_WITHDRAW)
                        .requestMatchers(HttpMethod.GET, "/api/leave/balance")
                        .hasAuthority(RbacPermissions.LEAVE_VIEW_BALANCE)
                        .requestMatchers(HttpMethod.GET, "/api/leave/tasks/**")
                        .hasAuthority(RbacPermissions.LEAVE_TASK_VIEW)
                        .requestMatchers(HttpMethod.POST, "/api/leave/tasks/{taskId}/claim")
                        .hasAuthority(RbacPermissions.LEAVE_TASK_CLAIM)
                        .requestMatchers(HttpMethod.POST, "/api/leave/tasks/{taskId}/approve")
                        .hasAuthority(RbacPermissions.LEAVE_TASK_APPROVE)
                        .requestMatchers(HttpMethod.POST, "/api/leave/tasks/{taskId}/resubmit")
                        .hasAuthority(RbacPermissions.LEAVE_TASK_RESUBMIT)
                        .requestMatchers(HttpMethod.GET, "/api/admin/rbac/**")
                        .hasAuthority(RbacPermissions.RBAC_VIEW)
                        .requestMatchers(HttpMethod.PUT, "/api/admin/rbac/**")
                        .hasAuthority(RbacPermissions.RBAC_MANAGE)
                        .requestMatchers(HttpMethod.POST, "/api/admin/deploy/zip")
                        .hasAuthority(RbacPermissions.PROCESS_DEPLOY)
                        .requestMatchers(HttpMethod.GET, "/api/admin/**")
                        .hasAuthority(RbacPermissions.PROCESS_VIEW)
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
