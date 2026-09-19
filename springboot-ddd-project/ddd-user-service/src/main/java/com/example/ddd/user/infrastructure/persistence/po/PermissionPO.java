package com.example.ddd.user.infrastructure.persistence.po;

import com.example.ddd.common.infrastructure.persistence.AbstractJpaAuditablePO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * PO：权限表。
 */
@Entity
@Table(name = "t_permission")
public class PermissionPO extends AbstractJpaAuditablePO {

    @Id
    @Column(name = "permission_id", length = 32, nullable = false)
    private String permissionId;

    @Column(name = "permission_code", length = 128, nullable = false)
    private String permissionCode;

    @Column(name = "permission_name", length = 128, nullable = false)
    private String permissionName;

    @Column(name = "resource", length = 64, nullable = false)
    private String resource;

    @Column(name = "action", length = 32, nullable = false)
    private String action;

    @Column(name = "description", length = 255)
    private String description;

    public String getPermissionId() { return permissionId; }
    public void setPermissionId(String permissionId) { this.permissionId = permissionId; }
    public String getPermissionCode() { return permissionCode; }
    public void setPermissionCode(String permissionCode) { this.permissionCode = permissionCode; }
    public String getPermissionName() { return permissionName; }
    public void setPermissionName(String permissionName) { this.permissionName = permissionName; }
    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
