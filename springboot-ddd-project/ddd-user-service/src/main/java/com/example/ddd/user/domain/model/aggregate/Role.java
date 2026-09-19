package com.example.ddd.user.domain.model.aggregate;

import com.example.ddd.common.domain.model.BaseAggregateRoot;
import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 聚合根 (Aggregate Root)：Role 角色。
 *
 * <p><b>为什么 Role 是独立聚合根而不是 User 聚合的一部分？</b>
 * <ul>
 *   <li>Role 有独立的生命周期：可以被创建、修改、删除，与具体 User 无关。</li>
 *   <li>Role 被多个 User 共享，如果放到 User 聚合内会造成数据冗余与一致性难题。</li>
 *   <li>Role 的修改（例如调整权限）不应该触发所有 User 的重新加载。</li>
 * </ul>
 *
 * <p><b>聚合边界：</b>Role 内部持有 permissionIds 集合（跨聚合引用，只保存 ID）。</p>
 *
 * <p><b>不变式：</b>
 * <ol>
 *   <li>roleCode 全局唯一。</li>
 *   <li>status = DISABLED 时不能被分配给用户。</li>
 * </ol>
 *
 * @author ddd-learning
 */
public class Role extends BaseAggregateRoot {

    private final String roleId;
    private final String roleCode;
    private String roleName;
    private String description;
    private boolean active;

    /** 该角色拥有的权限 ID 集合 */
    private final Set<String> permissionIds = new HashSet<>();

    public Role(String roleId, String roleCode, String roleName, String description, boolean active) {
        this.roleId = Objects.requireNonNull(roleId);
        this.roleCode = Objects.requireNonNull(roleCode);
        this.roleName = roleName;
        this.description = description;
        this.active = active;
    }

    /**
     * 修改角色基本信息。
     */
    public void updateInfo(String roleName, String description) {
        if (roleName != null && !roleName.isBlank()) this.roleName = roleName.trim();
        if (description != null) this.description = description;
    }

    /**
     * 授予权限。
     */
    public void grantPermission(String permissionId) {
        Objects.requireNonNull(permissionId);
        this.permissionIds.add(permissionId);
    }

    /**
     * 撤销权限。
     */
    public void revokePermission(String permissionId) {
        this.permissionIds.remove(permissionId);
    }

    /**
     * 停用角色（已分配的用户会失去该角色带来的权限）。
     */
    public void disable() {
        if (!this.active) {
            throw new BusinessException(ErrorCode.CONFLICT, "角色已停用");
        }
        this.active = false;
    }

    public void enable() {
        this.active = true;
    }

    @Override
    public String aggregateId() {
        return this.roleId;
    }

    public String getRoleId() { return roleId; }
    public String getRoleCode() { return roleCode; }
    public String getRoleName() { return roleName; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
    public Set<String> getPermissionIds() { return Collections.unmodifiableSet(permissionIds); }
}
