package com.example.active7.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.active7.mapper.SysAuthorityMapper;
import com.example.active7.mapper.SysUserMapper;
import com.example.active7.model.SysUser;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class DemoUserDetailsService implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final SysAuthorityMapper sysAuthorityMapper;

    public DemoUserDetailsService(SysUserMapper sysUserMapper, SysAuthorityMapper sysAuthorityMapper) {
        this.sysUserMapper = sysUserMapper;
        this.sysAuthorityMapper = sysAuthorityMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("账号不存在：" + username);
        }
        List<String> roleCodes = sysAuthorityMapper.selectRoleCodes(user.getUserId());
        List<String> permissionCodes = sysAuthorityMapper.selectPermissionCodes(user.getUserId());
        Set<String> authorityCodes = new LinkedHashSet<String>();
        for (String roleCode : roleCodes) {
            authorityCodes.add("ROLE_" + roleCode);
        }
        authorityCodes.addAll(permissionCodes);

        List<SimpleGrantedAuthority> authorities = new ArrayList<SimpleGrantedAuthority>();
        for (String authorityCode : authorityCodes) {
            authorities.add(new SimpleGrantedAuthority(authorityCode));
        }
        return new DemoUserDetails(user.getUserId(), user.getUsername(), user.getPassword(),
                Boolean.TRUE.equals(user.getEnabled()), user.getDisplayName(), user.getDepartment(),
                authorities);
    }
}
