package com.example.ddd.auth.infrastructure.security.permission;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自研方法级角色注解。
 *
 * <p><b>与 {@link RequiresPermission} 的区别：</b>
 * <ul>
 *   <li>{@code @RequiresRole}：粗粒度，只区分角色（USER / ADMIN / OPS）。</li>
 *   <li>{@code @RequiresPermission}：细粒度，按业务动作授权（{@code order:create}）。</li>
 * </ul>
 * 生产环境通常"角色 + 权限"并存：角色决定"能不能进后台"，权限决定"能干什么操作"。</p>
 *
 * <p><b>使用示例：</b>
 * <pre>
 * &#64;RequiresRole("ADMIN")
 * public void disableUser(...) { ... }
 *
 * &#64;RequiresRole(value = {"ADMIN", "OPS"}, logical = Logical.ANY)
 * public void viewAuditLog(...) { ... }
 * </pre>
 *
 * @see PermissionAspect
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresRole {

    /**
     * 需要的角色码集合，<b>不带 ROLE_ 前缀</b>（例如 {@code "ADMIN"}）。
     */
    String[] value();

    /**
     * 多角色之间的逻辑关系。
     */
    RequiresPermission.Logical logical() default RequiresPermission.Logical.ANY;
}
