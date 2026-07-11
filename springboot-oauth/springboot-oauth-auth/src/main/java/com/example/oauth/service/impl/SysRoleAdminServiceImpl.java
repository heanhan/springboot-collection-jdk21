package com.example.oauth.service.impl;

import com.example.oauth.common.exceptions.BizException;
import com.example.oauth.entity.SysRole;
import com.example.oauth.entity.SysRoleMenu;
import com.example.oauth.entity.SysRolePermission;
import com.example.oauth.repository.SysRoleMenuRepository;
import com.example.oauth.repository.SysRolePermissionRepository;
import com.example.oauth.repository.SysRoleRepository;
import com.example.oauth.repository.SysUserRoleRepository;
import com.example.oauth.service.SysRoleAdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色管理服务实现
 */
@Slf4j
@Service
public class SysRoleAdminServiceImpl implements SysRoleAdminService {

    private final SysRoleRepository roleRepository;
    private final SysRolePermissionRepository rolePermissionRepository;
    private final SysRoleMenuRepository roleMenuRepository;
    private final SysUserRoleRepository userRoleRepository;

    public SysRoleAdminServiceImpl(SysRoleRepository roleRepository,
                                   SysRolePermissionRepository rolePermissionRepository,
                                   SysRoleMenuRepository roleMenuRepository,
                                   SysUserRoleRepository userRoleRepository) {
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.roleMenuRepository = roleMenuRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    public Page<SysRole> page(String keyword, Pageable pageable) {
        if (StringUtils.hasText(keyword)) {
            return roleRepository.findByRoleNameContaining(keyword, pageable);
        }
        return roleRepository.findAll(pageable);
    }

    @Override
    public List<SysRole> listAll() {
        return roleRepository.findAll();
    }

    @Override
    public SysRole getById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "角色不存在: " + id));
    }

    @Override
    @Transactional
    public SysRole create(SysRole role) {
        if (!StringUtils.hasText(role.getRoleCode())) {
            throw new BizException(400, "角色编码不能为空");
        }
        if (roleRepository.existsByRoleCode(role.getRoleCode())) {
            throw new BizException(400, "角色编码已存在: " + role.getRoleCode());
        }
        role.setId(null);
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        return roleRepository.save(role);
    }

    @Override
    @Transactional
    public SysRole update(SysRole role) {
        if (role.getId() == null) {
            throw new BizException(400, "更新角色时 ID 不能为空");
        }
        SysRole existing = getById(role.getId());
        existing.setRoleName(role.getRoleName());
        existing.setDescription(role.getDescription());
        if (role.getStatus() != null) {
            existing.setStatus(role.getStatus());
        }
        // 角色编码一般不允许修改，此处保持原值
        return roleRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getById(id);
        userRoleRepository.deleteByRoleId(id);
        rolePermissionRepository.deleteByRoleId(id);
        roleMenuRepository.deleteByRoleId(id);
        roleRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        getById(roleId);
        rolePermissionRepository.deleteByRoleId(roleId);
        if (permissionIds != null) {
            permissionIds.stream().distinct().forEach(permId ->
                    rolePermissionRepository.save(new SysRolePermission(roleId, permId)));
        }
    }

    @Override
    public List<Long> getPermissionIds(Long roleId) {
        return rolePermissionRepository.findByRoleId(roleId).stream()
                .map(SysRolePermission::getPermissionId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        getById(roleId);
        roleMenuRepository.deleteByRoleId(roleId);
        if (menuIds != null) {
            menuIds.stream().distinct().forEach(menuId ->
                    roleMenuRepository.save(new SysRoleMenu(roleId, menuId)));
        }
    }

    @Override
    public List<Long> getMenuIds(Long roleId) {
        return roleMenuRepository.findByRoleId(roleId).stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toList());
    }
}
