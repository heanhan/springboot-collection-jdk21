package com.example.ddd.user.domain.model.aggregate;

import com.example.ddd.common.domain.model.BaseAggregateRoot;

import java.util.Objects;

/**
 * 聚合根 (Aggregate Root)：Permission 权限。
 *
 * <p>权限码采用 {@code resource:action} 格式，例如 {@code order:create}、{@code product:read}。</p>
 *
 * <p><b>为什么权限是独立聚合根？</b>
 * 权限由系统管理员统一维护，与角色是多对多关系；权限本身几乎不变（新增居多），
 * 独立成聚合便于缓存与集中管理。</p>
 *
 * @author ddd-learning
 */
public class Permission extends BaseAggregateRoot {

    private final String permissionId;
    private final String permissionCode;
    private String permissionName;
    private final String resource;
    private final String action;
    private String description;

    public Permission(String permissionId, String permissionCode, String permissionName,
                      String resource, String action, String description) {
        this.permissionId = Objects.requireNonNull(permissionId);
        this.permissionCode = Objects.requireNonNull(permissionCode);
        this.permissionName = permissionName;
        this.resource = Objects.requireNonNull(resource);
        this.action = Objects.requireNonNull(action);
        this.description = description;
    }

    public void updateInfo(String permissionName, String description) {
        if (permissionName != null && !permissionName.isBlank()) this.permissionName = permissionName.trim();
        if (description != null) this.description = description;
    }

    @Override
    public String aggregateId() {
        return this.permissionId;
    }

    public String getPermissionId() { return permissionId; }
    public String getPermissionCode() { return permissionCode; }
    public String getPermissionName() { return permissionName; }
    public String getResource() { return resource; }
    public String getAction() { return action; }
    public String getDescription() { return description; }
}
