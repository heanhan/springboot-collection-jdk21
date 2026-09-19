package com.example.ddd.user.infrastructure.persistence.repository;

import com.example.ddd.user.domain.model.aggregate.Role;
import com.example.ddd.user.domain.repository.RoleRepository;
import com.example.ddd.user.infrastructure.persistence.converter.RbacConverter;
import com.example.ddd.user.infrastructure.persistence.dao.RoleDao;
import com.example.ddd.user.infrastructure.persistence.dao.RolePermissionDao;
import com.example.ddd.user.infrastructure.persistence.po.RolePO;
import com.example.ddd.user.infrastructure.persistence.po.RolePermissionPO;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 仓储实现：Role。
 */
@Repository
public class RoleRepositoryImpl implements RoleRepository {

    private final RoleDao roleDao;
    private final RolePermissionDao rolePermissionDao;
    private final RbacConverter converter;

    public RoleRepositoryImpl(RoleDao roleDao, RolePermissionDao rolePermissionDao, RbacConverter converter) {
        this.roleDao = roleDao;
        this.rolePermissionDao = rolePermissionDao;
        this.converter = converter;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> findById(String roleId) {
        return roleDao.findById(roleId)
                .filter(po -> po.getDeleted() == null || po.getDeleted() == 0)
                .map(po -> converter.toDomain(po, loadPermissionIds(Set.of(po.getRoleId())).getOrDefault(po.getRoleId(), Set.of())));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> findByCode(String roleCode) {
        return roleDao.findByCode(roleCode)
                .map(po -> converter.toDomain(po, loadPermissionIds(Set.of(po.getRoleId())).getOrDefault(po.getRoleId(), Set.of())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> findByIds(Set<String> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return List.of();
        List<RolePO> pos = roleDao.findByIdIn(roleIds);
        Map<String, Set<String>> permMap = loadPermissionIds(roleIds);
        return pos.stream()
                .map(po -> converter.toDomain(po, permMap.getOrDefault(po.getRoleId(), Set.of())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> findAll() {
        List<RolePO> pos = roleDao.findAllNotDeleted();
        Set<String> ids = pos.stream().map(RolePO::getRoleId).collect(Collectors.toSet());
        Map<String, Set<String>> permMap = loadPermissionIds(ids);
        return pos.stream()
                .map(po -> converter.toDomain(po, permMap.getOrDefault(po.getRoleId(), Set.of())))
                .toList();
    }

    @Override
    @Transactional
    public void save(Role role) {
        RolePO po = converter.toPO(role);
        roleDao.findById(role.getRoleId()).ifPresent(existing -> {
            po.setCreateTime(existing.getCreateTime());
            po.setVersion(existing.getVersion());
        });
        roleDao.save(po);
        // 权限关联：先删后插
        rolePermissionDao.findByRoleIdIn(Set.of(role.getRoleId()))
                .forEach(rp -> rolePermissionDao.deleteById(rp.getId()));
        List<RolePermissionPO> relations = role.getPermissionIds().stream()
                .map(pid -> new RolePermissionPO(role.getRoleId(), pid))
                .toList();
        if (!relations.isEmpty()) {
            rolePermissionDao.saveAll(relations);
        }
    }

    private Map<String, Set<String>> loadPermissionIds(Set<String> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return Map.of();
        List<RolePermissionPO> list = rolePermissionDao.findByRoleIdIn(roleIds);
        Map<String, Set<String>> map = new java.util.HashMap<>();
        for (RolePermissionPO rp : list) {
            map.computeIfAbsent(rp.getRoleId(), k -> new HashSet<>()).add(rp.getPermissionId());
        }
        return map;
    }
}
