package com.example.dynamic.jpa.security;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


/**
 * @author zhaojh
 * spring-security权限管理的核心配置
 */
@EnableWebSecurity
@Configuration
public class SecurityConfig {

    /**
     * 权限过滤器（当前url所需要的访问权限）
     */
    @Resource
    private MyFilterInvocationSecurityMetadataSource filterMetadataSource;

    /**
     * 权限决策器
     */
    @Resource
    private MyAccessDecisionManager myAccessDecisionManager;

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
    UserJwtLoginFilter userJwtLoginFilter() throws Exception {
//        userJwtLoginFilter();
        UserJwtLoginFilter _userJwtLoginFilter = new UserJwtLoginFilter();
        _userJwtLoginFilter.setAuthenticationManager(authenticationManager());
//        _userJwtLoginFilter.setFilterProcessesUrl("/api/login");
        return _userJwtLoginFilter;
    }

    /**
     * 装载BCrypt密码编码器
     */
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
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
                web.ignoring().antMatchers("/doc.html")
                        .antMatchers("/swagger-ui.html")
                        .antMatchers("/resources/**")
                        .antMatchers("/webjars/**")
                        .antMatchers("/v3/api-docs/**")
                        //ios客户端的请求路径需要放开
                        .antMatchers("/iosapp/**")
                        .antMatchers("/swagger-resources/**");
            }
        };
    }

    /**
     * HttpSecurity包含了原数据（主要是url）
     * 通过withObjectPostProcessor将MyFilterInvocationSecurityMetadataSource和MyAccessDecisionManager注入进来
     * 此url先被MyFilterInvocationSecurityMetadataSource处理，然后 丢给 MyAccessDecisionManager处理
     * 如果不匹配，返回 MyAccessDeniedHandler
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.cors().and()
                // 由于使用的是JWT，我们这里不需要csrf
                .csrf().disable()
                .logout().disable()
                // 使用 JWT，关闭session
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                // 所有请求必须认证
                .and()
                .authorizeRequests()
                .anyRequest()
                // RBAC 动态 url 认证
                .access("@rbacauthorityservice.hasPermission(request,authentication)");
        // 无权访问 JSON 格式的数据
        httpSecurity.exceptionHandling().authenticationEntryPoint(jwtAuthenticationEntryPoint);
        httpSecurity.exceptionHandling().accessDeniedHandler(myAccessDeniedHandler);
        httpSecurity.authorizeRequests().withObjectPostProcessor(new ObjectPostProcessor<FilterSecurityInterceptor>() {
            @Override
            public <O extends FilterSecurityInterceptor> O postProcess(O o) {
                o.setSecurityMetadataSource(filterMetadataSource);
                o.setAccessDecisionManager(myAccessDecisionManager);
                return o;
            }

        });
        //用重写的Filter替换掉原有的UsernamePasswordAuthenticationFilter实现使用json 数据也可以登陆
//        httpSecurity.addFilterBefore(userJwtLoginFilter(),UsernamePasswordAuthenticationFilter.class);
        httpSecurity.addFilterAt(userJwtLoginFilter(), UsernamePasswordAuthenticationFilter.class);
        httpSecurity.addFilterAfter(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return httpSecurity.build();
    }
}
