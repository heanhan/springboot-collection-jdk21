package com.example.dynamic.jpa.security;

import com.example.dynamic.jpa.common.constants.SystemConstant;
import com.example.dynamic.jpa.system.entity.Tenant;
import com.example.dynamic.jpa.system.entity.User;
import com.example.dynamic.jpa.system.service.RoleService;
import com.example.dynamic.jpa.system.service.TenantService;
import com.example.dynamic.jpa.system.service.UserService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
public class AuthUserServiceImpl implements UserDetailsService {

    @Resource
    private UserService userService;

    @Resource
    private RoleService roleService;

    @Resource
    private TenantService tenantService;

    public static String identity(Integer tenantId, String username) {
        return tenantId + ":" + username;
    }

    @Override
    public UserDetails loadUserByUsername(String identity) throws UsernameNotFoundException {
        String[] parts = identity == null ? new String[0] : identity.split(":", 2);
        if (parts.length != 2 || !StringUtils.isNumeric(parts[0])) {
            throw new UsernameNotFoundException("用户不存在");
        }
        int tenantId = Integer.parseInt(parts[0]);
        if (tenantId < 0) {
            throw new UsernameNotFoundException("用户不存在");
        }
        User user = userService.getUserByName(parts[1], tenantId);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        if (tenantId != SystemConstant.SYSTEM_TENANT_ID) {
            Tenant tenant = tenantService.getTenantById(tenantId);
            if (tenant == null || !"ACTIVE".equals(tenant.getStatus())
                    || (tenant.getExpiresAt() != null && tenant.getExpiresAt().isBefore(LocalDateTime.now()))) {
                throw new UsernameNotFoundException("用户所属租户不存在或已停用");
            }
        }
        Set<GrantedAuthority> authorities = new HashSet<>();
        var roles = userService.listUserRoles(user.getId());
        if (roles.isEmpty()) {
            throw new UsernameNotFoundException("用户角色未配置");
        }
        for (var role : roles) {
            if (!java.util.Objects.equals(role.getTenantId(), tenantId)) {
                throw new UsernameNotFoundException("用户角色不属于当前租户");
            }
            if (StringUtils.isNotBlank(role.getRoleCode())) {
                authorities.add(new SimpleGrantedAuthority(role.getRoleCode()));
            }
            for (String permission : roleService.listRolePermissions(role.getId())) {
                authorities.add(new SimpleGrantedAuthority(permission));
            }
        }
        return new JwtUser(user, authorities);
    }
}
