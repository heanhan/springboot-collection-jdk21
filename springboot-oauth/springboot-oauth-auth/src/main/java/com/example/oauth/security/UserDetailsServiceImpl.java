package com.example.oauth.security;

import com.example.oauth.entity.SysUser;
import com.example.oauth.repository.SysPermissionRepository;
import com.example.oauth.repository.SysUserRepository;
import com.example.oauth.service.LoginAttemptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * UserDetailsService 实现
 * <p>
 * 从数据库加载用户基本信息，并通过关联表连表查询角色编码、权限编码，封装成 UserDetails
 */
@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserRepository sysUserRepository;
    private final SysPermissionRepository sysPermissionRepository;
    private final LoginAttemptService loginAttemptService;

    public UserDetailsServiceImpl(SysUserRepository sysUserRepository,
                                  SysPermissionRepository sysPermissionRepository,
                                  LoginAttemptService loginAttemptService) {
        this.sysUserRepository = sysUserRepository;
        this.sysPermissionRepository = sysPermissionRepository;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 检查是否被锁定
        if (loginAttemptService.isLocked(username)) {
            long remaining = loginAttemptService.getLockTimeRemaining(username);
            log.warn("用户 [{}] 已被锁定，剩余 {} 分钟", username, remaining);
            throw new UsernameNotFoundException("账号已被锁定，请 " + remaining + " 分钟后重试");
        }

        // 从数据库查询用户基本信息
        SysUser sysUser = sysUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户名不存在: " + username));

        // 通过关联表查询角色编码、权限编码
        Set<String> roleCodes = new HashSet<>(sysUserRepository.findRoleCodesByUserId(sysUser.getId()));
        Set<String> permCodes = new HashSet<>(sysPermissionRepository.findPermCodesByUserId(sysUser.getId()));

        log.info("加载用户信息: {}，角色数: {}，权限数: {}", username, roleCodes.size(), permCodes.size());
        return SecurityUser.build(sysUser, roleCodes, permCodes);
    }
}
