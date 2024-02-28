package com.example.dynamic.jpa.system.controller;


import com.example.dynamic.jpa.common.util.JwtUtil;
import com.example.dynamic.jpa.security.AuthUserServiceImpl;
import com.example.dynamic.jpa.security.LoginUser;
import com.example.dynamic.jpa.system.config.MyDataSource;
import com.example.dynamic.jpa.system.entity.DataSourceType;
import com.example.dynamic.jpa.system.entity.User;
import com.example.dynamic.jpa.system.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.result.ResultBody;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api")
@MyDataSource(type = DataSourceType.SYSTEM)
public class SystemController {

    @Resource
    public UserService userService;

    @Resource
    private BCryptPasswordEncoder passwordEncoder;

    @Resource
    private AuthUserServiceImpl authUserService;


    //添加租户账号
    @PostMapping(value = "/addTenantInfo")
    public ResultBody addTenantInfo(@RequestBody LoginUser loginUser, HttpServletRequest request){
        log.info("获取登陆的用户信息:{},{}",loginUser.getUsername(),loginUser.getPassword());
        User userByName = userService.getUserByName(loginUser.getUsername());
        Assert.isNull(userByName,"用户名已存在");
        User user=new User();
        user.setUsername(loginUser.getUsername());
        user.setNickname(loginUser.getUsername());
        user.setPhone("15810011781");
        user.setRoleId(1);
        user.setTenantId(1);
        user.setType(1);
        user.setCreateTime(LocalDateTime.now());
        user.setStatus(1);
//        user.setPassword(passwordEncoder.encode(loginUser.getPassword()));
        userService.addUser(user);
        return ResultBody.success("用户注册成功！");
    }

    //用户登录
    @PostMapping(value = "/login")
    public ResultBody login(@RequestBody LoginUser loginUser, HttpServletRequest request){
        log.info("获取登陆的用户信息");
        UserDetails userDetails = authUserService.loadUserByUsername(loginUser.getUsername());
        if(!passwordEncoder.matches(loginUser.getPassword(),userDetails.getPassword())){
            log.info("账号有误");
        }
        if(!userDetails.isEnabled()){
            log.info("用户账号被禁用");
        }
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String  token = JwtUtil.createToken(loginUser.getUsername(),"123456",null);
        return ResultBody.success(token);
    }
}
