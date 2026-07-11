package com.example.oauth.service.impl;

import com.example.oauth.common.exceptions.BizException;
import com.example.oauth.entity.SysPermission;
import com.example.oauth.repository.SysPermissionRepository;
import com.example.oauth.repository.SysRolePermissionRepository;
import com.example.oauth.security.DynamicPermissionCache;
import com.example.oauth.service.SysPermissionAdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 权限管理服务实现
 * <p>
 * 接口权限（type=3）的增删改会影响动态鉴权规则，变更后调用
 * {@link DynamicPermissionCache#refresh()} 重新加载规则表。
 */
@Slf4j
@Service
public class SysPermissionAdminServiceImpl implements SysPermissionAdminService {

    private final SysPermissionRepository permissionRepository;
    private final SysRolePermissionRepository rolePermissionRepository;
    private final DynamicPermissionCache dynamicPermissionCache;

    public SysPermissionAdminServiceImpl(SysPermissionRepository permissionRepository,
                                         SysRolePermissionRepository rolePermissionRepository,
                                         DynamicPermissionCache dynamicPermissionCache) {
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.dynamicPermissionCache = dynamicPermissionCache;
    }

    @Override
    public Page<SysPermission> page(String keyword, Pageable pageable) {
        if (StringUtils.hasText(keyword)) {
            return permissionRepository.findByPermNameContaining(keyword, pageable);
        }
        return permissionRepository.findAll(pageable);
    }

    @Override
    public List<SysPermission> listAll() {
        return permissionRepository.findAll();
    }

    @Override
    public SysPermission getById(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "权限不存在: " + id));
    }

    @Override
    @Transactional
    public SysPermission create(SysPermission permission) {
        if (!StringUtils.hasText(permission.getPermCode())) {
            throw new BizException(400, "权限编码不能为空");
        }
        if (permissionRepository.existsByPermCode(permission.getPermCode())) {
            throw new BizException(400, "权限编码已存在: " + permission.getPermCode());
        }
        permission.setId(null);
        if (permission.getStatus() == null) {
            permission.setStatus(1);
        }
        SysPermission saved = permissionRepository.save(permission);
        dynamicPermissionCache.refresh();
        return saved;
    }

    @Override
    @Transactional
    public SysPermission update(SysPermission permission) {
        if (permission.getId() == null) {
            throw new BizException(400, "更新权限时 ID 不能为空");
        }
        SysPermission existing = getById(permission.getId());
        existing.setPermName(permission.getPermName());
        existing.setType(permission.getType());
        existing.setUrl(permission.getUrl());
        existing.setMethod(permission.getMethod());
        existing.setDescription(permission.getDescription());
        if (permission.getStatus() != null) {
            existing.setStatus(permission.getStatus());
        }
        // 权限编码一般不允许修改，此处保持原值
        SysPermission saved = permissionRepository.save(existing);
        dynamicPermissionCache.refresh();
        return saved;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getById(id);
        rolePermissionRepository.deleteByPermissionId(id);
        permissionRepository.deleteById(id);
        dynamicPermissionCache.refresh();
    }
}
