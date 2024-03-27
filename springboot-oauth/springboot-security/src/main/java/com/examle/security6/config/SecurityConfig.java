package com.examle.security6.config;

import com.examle.security6.config.security.AuthAccessDeniedHandler;
import com.examle.security6.config.security.AuthEntryPointHandler;
import com.examle.security6.filter.JwtAuthenticationFilter;
import com.examle.security6.utils.SpringContextUtils;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * spring security6的配置文件
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Resource
    private UserDetailsService userDetailsService;

    @Resource
    private AuthAccessDeniedHandler authAccessDeniedHandler;

    @Resource
    private AuthEntryPointHandler authEntryPointHandler;


    /**
     * 密码编码器
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 身份验证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * 处理身份验证
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
        daoAuthenticationProvider.setUserDetailsService(userDetailsService);
        return daoAuthenticationProvider;
    }

    /**
     * @Description: 配置SecurityFilterChain过滤器链
     * @Param: HttpSecurity
     * @Return: SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests
                .requestMatchers(HttpMethod.POST, "/security/api/user/login").permitAll() //登录放行
                .requestMatchers(HttpMethod.POST,"/security/api/user/register").permitAll() //注册放行
                .anyRequest().authenticated()
        );
         httpSecurity.authenticationProvider(authenticationProvider());

        //禁用登录页面
        httpSecurity.formLogin(AbstractHttpConfigurer::disable);
        //禁用登出页面
        httpSecurity.logout(AbstractHttpConfigurer::disable);
        //禁用session
        httpSecurity.sessionManagement(AbstractHttpConfigurer::disable);
        //禁用httpBasic
        httpSecurity.httpBasic(AbstractHttpConfigurer::disable);
        //禁用csrf保护
        httpSecurity.csrf(AbstractHttpConfigurer::disable);
        //通过上下文获取AuthenticationManager
        AuthenticationManager authenticationManager = SpringContextUtils.getBean("authenticationManager");
        //添加自定义token验证过滤器
        httpSecurity.addFilterBefore(new JwtAuthenticationFilter(authenticationManager), UsernamePasswordAuthenticationFilter.class);
        //自定义处理器
        httpSecurity.exceptionHandling(exceptionHandling -> exceptionHandling
                .accessDeniedHandler(authAccessDeniedHandler) //处理用户权限不足
                .authenticationEntryPoint(authEntryPointHandler) //处理用户未登录（未携带token）
        );
        return httpSecurity.build();
    }

    /**
     * 静态资源放行
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
                "/doc.html",
                "/doc.html/**",
                "/v3/api-docs",
                "/v3/api-docs/**",
                "/webjars/**",
                "/authenticate",
                "/swagger-ui.html/**",
                "/swagger-resources",
                "/swagger-resources/**"
        );
    }




}
