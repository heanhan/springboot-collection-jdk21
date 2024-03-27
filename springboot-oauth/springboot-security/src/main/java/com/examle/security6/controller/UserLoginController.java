package com.examle.security6.controller;

import com.examle.security6.model.dto.UserLoginDTO;
import com.examle.security6.model.vo.UserLogin;
import com.examle.security6.utils.JwtUtils;
import com.example.common.result.ResultBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xnio.Result;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequestMapping("/api/user")
@RestController
@RequiredArgsConstructor
public class UserLoginController {

    private final AuthenticationManager authenticationManager;

    private final JwtUtils jwtUtils;

    @PostMapping(value = "/login")
    public ResultBody login(@RequestBody UserLogin userLogin) {
        try {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userLogin.getUsername(), userLogin.getPassword());
            Authentication authentication = authenticationManager.authenticate(auth);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            //获取用户权限信息
            String authorityString = "";
            Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
            for (GrantedAuthority authority : authorities) {
                authorityString = authority.getAuthority();
            }

            //用户身份验证成功，生成并返回jwt令牌
            Map<String, Object> claims = new HashMap<>();
            claims.put("username", userDetails.getUsername());
            claims.put("authorityString", authorityString);
            String jwtToken = jwtUtils.getJwt(claims);
            return ResultBody.success(jwtToken);
        } catch (Exception ex) {
            //用户身份验证失败，返回登陆失败提示
            return ResultBody.error("用户名或密码错误！");
        }
    }



}
