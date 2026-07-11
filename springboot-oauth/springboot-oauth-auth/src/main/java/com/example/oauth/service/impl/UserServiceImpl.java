package com.example.oauth.service.impl;

import com.example.oauth.common.exceptions.BizException;
import com.example.oauth.dto.ChangePasswordDTO;
import com.example.oauth.dto.ProfileUpdateDTO;
import com.example.oauth.dto.RegisterDTO;
import com.example.oauth.entity.SysMenu;
import com.example.oauth.entity.SysUser;
import com.example.oauth.entity.SysUserRole;
import com.example.oauth.repository.SysMenuRepository;
import com.example.oauth.repository.SysPermissionRepository;
import com.example.oauth.repository.SysRoleRepository;
import com.example.oauth.repository.SysUserRepository;
import com.example.oauth.repository.SysUserRoleRepository;
import com.example.oauth.service.MenuService;
import com.example.oauth.service.UserService;
import com.example.oauth.vo.MenuVO;
import com.example.oauth.vo.UserInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 用户业务服务实现
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    /** 注册时默认绑定的角色编码 */
    private static final String DEFAULT_ROLE_CODE = "USER";

    private final SysUserRepository sysUserRepository;
    private final SysMenuRepository sysMenuRepository;
    private final SysPermissionRepository sysPermissionRepository;
    private final SysRoleRepository sysRoleRepository;
    private final SysUserRoleRepository sysUserRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final MenuService menuService;

    public UserServiceImpl(SysUserRepository sysUserRepository,
                           SysMenuRepository sysMenuRepository,
                           SysPermissionRepository sysPermissionRepository,
                           SysRoleRepository sysRoleRepository,
                           SysUserRoleRepository sysUserRoleRepository,
                           PasswordEncoder passwordEncoder,
                           MenuService menuService) {
        this.sysUserRepository = sysUserRepository;
        this.sysMenuRepository = sysMenuRepository;
        this.sysPermissionRepository = sysPermissionRepository;
        this.sysRoleRepository = sysRoleRepository;
        this.sysUserRoleRepository = sysUserRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.menuService = menuService;
    }

    @Override
    @Transactional
    public SysUser register(RegisterDTO dto) {
        if (sysUserRepository.existsByUsername(dto.getUsername())) {
            throw new BizException(400, "用户名已存在: " + dto.getUsername());
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setGender(0);
        user.setStatus(1);
        SysUser saved = sysUserRepository.save(user);

        // 默认绑定 USER 角色（若存在）
        sysRoleRepository.findByRoleCode(DEFAULT_ROLE_CODE).ifPresent(role ->
                sysUserRoleRepository.save(new SysUserRole(saved.getId(), role.getId())));
        return saved;
    }

    @Override
    @Transactional
    public SysUser updateProfile(Long userId, ProfileUpdateDTO dto) {
        SysUser user = getRequiredUser(userId);
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        if (dto.getGender() != null) {
            user.setGender(dto.getGender());
        }
        return sysUserRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        SysUser user = getRequiredUser(userId);
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BizException(400, "旧密码不正确");
        }
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new BizException(400, "新密码不能与旧密码相同");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        sysUserRepository.save(user);
    }

    private SysUser getRequiredUser(Long userId) {
        return sysUserRepository.findById(userId)
                .orElseThrow(() -> new BizException(404, "用户不存在: " + userId));
    }

    @Override
    public SysUser findByUsername(String username) {
        return sysUserRepository.findByUsername(username).orElse(null);
    }

    @Override
    public SysUser findById(Long id) {
        return sysUserRepository.findById(id).orElse(null);
    }

    @Override
    public UserInfoVO buildUserInfoVO(SysUser sysUser) {
        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(sysUser.getId());
        vo.setUsername(sysUser.getUsername());
        vo.setNickname(sysUser.getNickname());
        vo.setAvatar(sysUser.getAvatar());
        vo.setEmail(sysUser.getEmail());
        vo.setPhone(sysUser.getPhone());
        vo.setLoginTime(new Date());

        // 通过关联表连表查询角色编码、权限编码
        Set<String> roles = new HashSet<>(sysUserRepository.findRoleCodesByUserId(sysUser.getId()));
        Set<String> permissions = new HashSet<>(sysPermissionRepository.findPermCodesByUserId(sysUser.getId()));

        vo.setRoles(roles);
        vo.setPermissions(permissions);
        return vo;
    }

    @Override
    public List<MenuVO> getUserMenuTree(Long userId) {
        List<SysMenu> menus = sysMenuRepository.findMenusByUserId(userId);
        return menuService.buildMenuTree(menus);
    }
}
