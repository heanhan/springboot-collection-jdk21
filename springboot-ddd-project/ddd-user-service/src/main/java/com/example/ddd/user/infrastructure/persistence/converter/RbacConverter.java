package com.example.ddd.user.infrastructure.persistence.converter;

import com.example.ddd.user.domain.model.aggregate.Permission;
import com.example.ddd.user.domain.model.aggregate.Role;
import com.example.ddd.user.infrastructure.persistence.po.PermissionPO;
import com.example.ddd.user.infrastructure.persistence.po.RolePO;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 转换器：Role / Permission。
 */
@Component
public class RbacConverter {

    public Role toDomain(RolePO po, Set<String> permissionIds) {
        Role role = new Role(po.getRoleId(), po.getRoleCode(), po.getRoleName(),
                po.getDescription(), "ACTIVE".equals(po.getStatus()));
        if (permissionIds != null) {
            permissionIds.forEach(role::grantPermission);
        }
        return role;
    }

    public RolePO toPO(Role role) {
        RolePO po = new RolePO();
        po.setRoleId(role.getRoleId());
        po.setRoleCode(role.getRoleCode());
        po.setRoleName(role.getRoleName());
        po.setDescription(role.getDescription());
        po.setStatus(role.isActive() ? "ACTIVE" : "DISABLED");
        return po;
    }

    public Permission toDomain(PermissionPO po) {
        return new Permission(po.getPermissionId(), po.getPermissionCode(), po.getPermissionName(),
                po.getResource(), po.getAction(), po.getDescription());
    }

    public PermissionPO toPO(Permission permission) {
        PermissionPO po = new PermissionPO();
        po.setPermissionId(permission.getPermissionId());
        po.setPermissionCode(permission.getPermissionCode());
        po.setPermissionName(permission.getPermissionName());
        po.setResource(permission.getResource());
        po.setAction(permission.getAction());
        po.setDescription(permission.getDescription());
        return po;
    }
}
