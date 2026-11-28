package com.example.active7.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * 在 Spring Security 用户对象上补充业务字段，避免页面再次查用户表。
 */
public class DemoUserDetails extends User {

    private static final long serialVersionUID = 1L;

    private final String userId;
    private final String displayName;
    private final String department;

    public DemoUserDetails(String userId, String username, String password,
                           boolean enabled, String displayName, String department,
                           Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, true, true, true, authorities);
        this.userId = userId;
        this.displayName = displayName;
        this.department = department;
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDepartment() {
        return department;
    }
}
