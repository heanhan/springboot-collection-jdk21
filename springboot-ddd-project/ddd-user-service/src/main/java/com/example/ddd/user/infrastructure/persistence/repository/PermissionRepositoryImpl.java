package com.example.ddd.user.infrastructure.persistence.repository;

import com.example.ddd.user.domain.model.aggregate.Permission;
import com.example.ddd.user.domain.repository.PermissionRepository;
import com.example.ddd.user.infrastructure.persistence.converter.RbacConverter;
import com.example.ddd.user.infrastructure.persistence.dao.PermissionDao;
import com.example.ddd.user.infrastructure.persistence.po.PermissionPO;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 仓储实现：Permission。
 */
@Repository
public class PermissionRepositoryImpl implements PermissionRepository {

    private final PermissionDao permissionDao;
    private final RbacConverter converter;

    public PermissionRepositoryImpl(PermissionDao permissionDao, RbacConverter converter) {
        this.permissionDao = permissionDao;
        this.converter = converter;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Permission> findById(String permissionId) {
        return permissionDao.findById(permissionId)
                .filter(po -> po.getDeleted() == null || po.getDeleted() == 0)
                .map(converter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Permission> findByCode(String permissionCode) {
        return permissionDao.findByCode(permissionCode).map(converter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> findByIds(Set<String> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) return List.of();
        return permissionDao.findByIdIn(permissionIds).stream().map(converter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> findAll() {
        return permissionDao.findAllNotDeleted().stream().map(converter::toDomain).toList();
    }

    @Override
    @Transactional
    public void save(Permission permission) {
        PermissionPO po = converter.toPO(permission);
        permissionDao.findById(permission.getPermissionId()).ifPresent(existing -> {
            po.setCreateTime(existing.getCreateTime());
            po.setVersion(existing.getVersion());
        });
        permissionDao.save(po);
    }
}
