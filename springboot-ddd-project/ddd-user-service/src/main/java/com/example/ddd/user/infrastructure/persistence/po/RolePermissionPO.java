package com.example.ddd.user.infrastructure.persistence.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * PO：角色-权限关联表。
 */
@Entity
@Table(name = "t_role_permission")
public class RolePermissionPO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "role_id", length = 32, nullable = false)
    private String roleId;

    @Column(name = "permission_id", length = 32, nullable = false)
    private String permissionId;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    public RolePermissionPO() {}

    public RolePermissionPO(String roleId, String permissionId) {
        this.roleId = roleId;
        this.permissionId = permissionId;
        this.createTime = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getRoleId() { return roleId; }
    public String getPermissionId() { return permissionId; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setId(Long id) { this.id = id; }
    public void setRoleId(String roleId) { this.roleId = roleId; }
    public void setPermissionId(String permissionId) { this.permissionId = permissionId; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
