package com.example.oauth.security;

import com.example.oauth.entity.SysUser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Spring Security UserDetails 实现
 * <p>
 * 封装 SysUser 及其角色、权限信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecurityUser implements UserDetails {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private Integer status;

    /** 角色编码集合 */
    private Set<String> roles;

    /** 权限编码集合 */
    private Set<String> permissions;

    /** Spring Security 权限集合 */
    private Collection<? extends GrantedAuthority> authorities;

    /**
     * 从 SysUser 及其角色编码、权限编码集合构建 SecurityUser
     * <p>
     * 角色与权限不再通过实体关系导航获取，而是由调用方通过 Repository 连表查询后传入。
     *
     * @param sysUser   用户基本信息
     * @param roleCodes 角色编码集合（不含 ROLE_ 前缀）
     * @param permCodes 权限编码集合
     */
    public static SecurityUser build(SysUser sysUser, Set<String> roleCodes, Set<String> permCodes) {
        Set<String> roles = roleCodes != null ? new HashSet<>(roleCodes) : new HashSet<>();
        Set<String> permissions = permCodes != null ? new HashSet<>(permCodes) : new HashSet<>();

        SecurityUser securityUser = new SecurityUser();
        securityUser.setUserId(sysUser.getId());
        securityUser.setUsername(sysUser.getUsername());
        securityUser.setPassword(sysUser.getPassword());
        securityUser.setNickname(sysUser.getNickname());
        securityUser.setAvatar(sysUser.getAvatar());
        securityUser.setEmail(sysUser.getEmail());
        securityUser.setPhone(sysUser.getPhone());
        securityUser.setStatus(sysUser.getStatus());
        securityUser.setRoles(roles);
        securityUser.setPermissions(permissions);
        // 根据角色/权限编码构建 Spring Security 权限集合
        securityUser.rebuildAuthorities();
        return securityUser;
    }

    /**
     * 根据 roles/permissions 重建 Spring Security 权限集合。
     * <p>
     * authorities 字段的 getter 带 @JsonIgnore，不会被序列化；从 Redis 缓存
     * 或 JWT Claims 反序列化恢复 SecurityUser 后，需调用本方法重建 authorities。
     */
    public void rebuildAuthorities() {
        Set<GrantedAuthority> authSet = new HashSet<>();
        if (this.roles != null) {
            this.roles.forEach(roleCode -> authSet.add(new SimpleGrantedAuthority("ROLE_" + roleCode)));
        }
        if (this.permissions != null) {
            this.permissions.forEach(permCode -> authSet.add(new SimpleGrantedAuthority(permCode)));
        }
        this.authorities = authSet;
    }

    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @JsonIgnore
    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isEnabled() {
        return status != null && status == 1;
    }
}
