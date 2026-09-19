package com.example.ddd.user.infrastructure.persistence.po;

import com.example.ddd.common.infrastructure.persistence.AbstractJpaAuditablePO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * PO：角色表。
 */
@Entity
@Table(name = "t_role")
public class RolePO extends AbstractJpaAuditablePO {

    @Id
    @Column(name = "role_id", length = 32, nullable = false)
    private String roleId;

    @Column(name = "role_code", length = 64, nullable = false)
    private String roleCode;

    @Column(name = "role_name", length = 64, nullable = false)
    private String roleName;

    @Column(name = "description", length = 255)
    private String description;

    /** ACTIVE / DISABLED */
    @Column(name = "status", length = 16, nullable = false)
    private String status;

    public String getRoleId() { return roleId; }
    public void setRoleId(String roleId) { this.roleId = roleId; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
