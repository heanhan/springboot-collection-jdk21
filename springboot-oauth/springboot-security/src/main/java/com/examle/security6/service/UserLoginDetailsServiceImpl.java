package com.examle.security6.service;

import com.examle.security6.dao.AuthorityMapper;
import com.examle.security6.dao.UserMapper;
import com.examle.security6.entity.AuthorityEntity;
import com.examle.security6.entity.UserEntity;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.StringJoiner;

@Service
public class UserLoginDetailsServiceImpl implements UserDetailsService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private AuthorityMapper authorityMapper;

    /**
     * 加载用户信息
     * @param username  登录账号
     * @return
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //根据用户查插用户是否存在
        UserEntity userEntity = userMapper.selectUserByUsername(username);
        List<AuthorityEntity> authorities = authorityMapper.selectAuthorityByUsername(username);
        StringJoiner stringJoiner = new StringJoiner(",", "", "");
        authorities.forEach(authority -> stringJoiner.add(authority.getAuthority()));
        return new User(userEntity.getUsername(), userEntity.getPassword(),
                AuthorityUtils.commaSeparatedStringToAuthorityList(stringJoiner.toString())
        );

    }
}
