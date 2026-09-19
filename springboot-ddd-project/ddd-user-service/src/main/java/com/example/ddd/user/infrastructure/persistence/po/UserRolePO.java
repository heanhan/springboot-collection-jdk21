package com.example.ddd.user.infrastructure.persistence.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * PO：用户-角色关联表。
 *
 * <p>纯关联表，不需要审计字段，也不作为聚合根，只是一张关系表。</p>
 */
@Entity
@Table(name = "t_user_role")
public class UserRolePO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", length = 32, nullable = false)
    private String userId;

    @Column(name = "role_id", length = 32, nullable = false)
    private String roleId;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    public UserRolePO() {}

    public UserRolePO(String userId, String roleId) {
        this.userId = userId;
        this.roleId = roleId;
        this.createTime = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getRoleId() { return roleId; }
    public void setRoleId(String roleId) { this.roleId = roleId; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
