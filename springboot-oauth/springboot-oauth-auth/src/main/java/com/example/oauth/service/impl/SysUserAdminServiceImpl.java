package com.example.oauth.service.impl;

import com.example.oauth.common.exceptions.BizException;
import com.example.oauth.dto.UserSaveDTO;
import com.example.oauth.entity.SysUser;
import com.example.oauth.entity.SysUserRole;
import com.example.oauth.repository.SysUserRepository;
import com.example.oauth.repository.SysUserRoleRepository;
import com.example.oauth.service.SysUserAdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理服务实现
 */
@Slf4j
@Service
public class SysUserAdminServiceImpl implements SysUserAdminService {

    private final SysUserRepository userRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public SysUserAdminServiceImpl(SysUserRepository userRepository,
                                   SysUserRoleRepository userRoleRepository,
                                   PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Page<SysUser> page(String keyword, Pageable pageable) {
        if (StringUtils.hasText(keyword)) {
            return userRepository.findByUsernameContaining(keyword, pageable);
        }
        return userRepository.findAll(pageable);
    }

    @Override
    public SysUser getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "用户不存在: " + id));
    }

    @Override
    @Transactional
    public SysUser create(UserSaveDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new BizException(400, "用户名已存在: " + dto.getUsername());
        }
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new BizException(400, "新增用户时密码不能为空");
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setAvatar(dto.getAvatar());
        user.setGender(dto.getGender());
        user.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public SysUser update(UserSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(400, "更新用户时 ID 不能为空");
        }
        SysUser user = getById(dto.getId());
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setAvatar(dto.getAvatar());
        user.setGender(dto.getGender());
        if (dto.getStatus() != null) {
            user.setStatus(dto.getStatus());
        }
        // 密码非空才更新
        if (StringUtils.hasText(dto.getPassword())) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getById(id);
        userRoleRepository.deleteByUserId(id);
        userRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void changeStatus(Long id, Integer status) {
        SysUser user = getById(id);
        user.setStatus(status);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        if (!StringUtils.hasText(newPassword)) {
            throw new BizException(400, "新密码不能为空");
        }
        SysUser user = getById(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        getById(userId);
        userRoleRepository.deleteByUserId(userId);
        if (roleIds != null) {
            roleIds.stream().distinct().forEach(roleId ->
                    userRoleRepository.save(new SysUserRole(userId, roleId)));
        }
    }

    @Override
    public List<Long> getRoleIds(Long userId) {
        return userRoleRepository.findByUserId(userId).stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
    }
}
