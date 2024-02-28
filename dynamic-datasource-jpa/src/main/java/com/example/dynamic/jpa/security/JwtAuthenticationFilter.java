package com.example.dynamic.jpa.security;

import com.example.dynamic.jpa.common.util.JwtUtil;
import com.example.dynamic.jpa.exception.ExceptionCode;
import com.example.dynamic.jpa.exception.JwtException;
import com.example.dynamic.jpa.system.config.LoginInfoHolder;
import com.example.dynamic.jpa.system.vo.LoginInfo;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * token的校验
 * OncePerRequestFilter， 确保在一次请求只通过一次filter，而不需要重复执行，
 * 从http头的Authorization 项读取token数据，然后用Jwts包提供的方法校验token的合法性。
 * 如果校验通过，就认为这是一个取得授权的合法请求
 *
 * @author zhaojh
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Resource(name = "authUserServiceImpl")
    private UserDetailsService userDetailsService;

    @Resource
    private AuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws IOException, ServletException {
        String tokenHeader = request.getHeader(JwtUtil.TOKEN_HEADER);
        // 如果请求头中没有Authorization信息则直接放行了
        if (tokenHeader == null || StringUtils.isEmpty(tokenHeader)) {
            if (request.getRequestURI().contains("/iosapp/")) {
                //手机客户端请求的参数处理
                String tenantId = request.getParameter("tenant_id");
//                ServletInputStream inputStream = wrappedRequest.getInputStream();
//                Object ioInput=inputStream;
                request.setAttribute("tenant", tenantId);
                filterChain.doFilter(request, response);
                return;
            }
//            else {
//                BodyReaderHttpServletRequestWrapper wrappedRequest = new BodyReaderHttpServletRequestWrapper(request);
//                //针对ios设备的请求路径  不做json话处理
////                String ContentType = request.getHeader("Content-Type");
////                && (ContentType == null || ContentType.contains("application/json"))
//                if (!request.getRequestURI().contains("/iosapp/") ) {
//                    String body = wrappedRequest.getBodyStr();
//                    if (StringUtils.isNotBlank(body)) {
//                        JSONObject parse = JSONObject.parseObject(body);
//                        //未处理的账号名 需要处理  如admin@aaa,@之前是账号，之后是租户
//                        if (!ObjectUtils.isEmpty(parse)) {
//                            if (parse.get("username") != null) {
//                                String proUserName = parse.get("username").toString();
//                                String tenant = proUserName.substring(proUserName.indexOf("@") + 1, proUserName.length());
//                                String username = proUserName.substring(0, proUserName.indexOf("@"));
//                                wrappedRequest.setAttribute("tenant", tenant);
//                                wrappedRequest.setAttribute("username", username);
//                            }
//                        }
//                    }
//                }
//                filterChain.doFilter(wrappedRequest, response);
//                return;
//            }
        }
        try {
            // 如果不是以指定字符串开头则直接返回失败
            if (!tokenHeader.startsWith(JwtUtil.TOKEN_PREFIX)) {
                throw new JwtException("token格式错误");
            }
            SecurityContextHolder.getContext().setAuthentication(getAuthentication(tokenHeader));

            filterChain.doFilter(request, response);
        } catch (JwtException e) {
            // token错误直接返回信息
            log.error(e.getMessage(), JwtAuthenticationFilter.class);
            SecurityContextHolder.clearContext();
            this.authenticationEntryPoint.commence(request, response, e);
        }
    }

    /**
     * 这里从token中获取用户信息
     */
    private UsernamePasswordAuthenticationToken getAuthentication(String tokenHeader) {
        String token = tokenHeader.replace(JwtUtil.TOKEN_PREFIX, JwtUtil.EMPTY_STRING);
        try {
            String realusername=null;
            String username=null;
            username = JwtUtil.getTokenBody(token, JwtUtil.TOKEN_SECRET).getSubject();
            if(username.contains("@")){
                //真实账号
                realusername=username.substring(0,username.indexOf("@"));
                //租户
            }else{
                realusername=username;
                LoginInfo loginInfo = LoginInfo.getLoginInfoByToken(token);
                LoginInfoHolder.setTenant(loginInfo);
            }
            if (username != null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(realusername);
                if (userDetails != null) {
                    return new UsernamePasswordAuthenticationToken(realusername, null, userDetails.getAuthorities());
                }
            }
        } catch (ExpiredJwtException e) {
            log.error(e.getMessage(), JwtAuthenticationFilter.class);
            throw new JwtException(ExceptionCode.TOKEN_EXPIRE.getCode(), ExceptionCode.TOKEN_EXPIRE.getMsg());
        } catch (MalformedJwtException e) {
            log.error(e.getMessage(), JwtAuthenticationFilter.class);
            throw new JwtException(ExceptionCode.TOKEN_ERROR.getCode(), ExceptionCode.TOKEN_ERROR.getMsg());
        } catch (Exception e) {
            throw new JwtException(ExceptionCode.TOKEN_ERROR.getCode(), ExceptionCode.TOKEN_ERROR.getMsg());
        }
        return null;
    }

}