package com.example.dynamic.jpa.security;

import com.example.dynamic.jpa.system.entity.Role;
import com.example.dynamic.jpa.system.entity.User;
import com.example.dynamic.jpa.system.service.RoleService;
import com.example.dynamic.jpa.system.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * @Author : zhaojh
 * @Description : 用户信息加载服务
 */
@Service
public class AuthUserServiceImpl implements UserDetailsService {

    @Resource
    private UserService userService;

    @Resource
    private RoleService roleService;

    /**
     * 实现了UserDetailsService接口中的loadUserByUsername方法
     * 执行登录,构建Authentication对象必须的信息,
     * 如果用户不存在，则抛出UsernameNotFoundException异常
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userService.getUserByName(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
        Role role = roleService.getRoleById(user.getRoleId());
        grantedAuthorities.add(new SimpleGrantedAuthority(role.getRoleName()));
        return new JwtUser(user, grantedAuthorities);
    }
}
