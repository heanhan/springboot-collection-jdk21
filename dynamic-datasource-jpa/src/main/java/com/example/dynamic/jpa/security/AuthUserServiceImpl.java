package com.example.dynamic.jpa.security;

import com.example.dynamic.jpa.common.constants.SystemConstant;
import com.example.dynamic.jpa.system.entity.Role;
import com.example.dynamic.jpa.system.entity.Tenant;
import com.example.dynamic.jpa.system.entity.User;
import com.example.dynamic.jpa.system.service.RoleService;
import com.example.dynamic.jpa.system.service.TenantService;
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

    @Resource
    private TenantService tenantService;

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
        if (user.getTenantId() == null || user.getTenantId() < 0 || user.getRoleId() == null) {
            throw new UsernameNotFoundException("用户租户或角色未配置");
        }
        Role role = roleService.getRoleById(user.getRoleId());
        if (role == null || Boolean.TRUE.equals(role.getIsDel())
                || org.apache.commons.lang3.StringUtils.isBlank(role.getRoleName())) {
            throw new UsernameNotFoundException("用户角色不存在或已停用");
        }
        if (user.getTenantId() != SystemConstant.SYSTEM_TENANT_ID) {
            Tenant tenant = tenantService.getTenantById(user.getTenantId());
            if (tenant == null || Boolean.TRUE.equals(tenant.getIsDel())) {
                throw new UsernameNotFoundException("用户所属租户不存在或已停用");
            }
        }
        grantedAuthorities.add(new SimpleGrantedAuthority(role.getRoleName()));
        return new JwtUser(user, grantedAuthorities);
    }
}
