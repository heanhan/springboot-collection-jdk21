package com.example.ddd.auth.application.port;

import java.util.Set;

/**
 * 出站端口 (Outbound Port)：用户角色权限网关。
 *
 * <p>auth-service 不持有 Role / Permission 数据（那是 user-service 的），
 * 但登录时又必须把角色权限放进 JWT，因此通过 Feign 调用 user-service。
 * 这里抽象出接口，实现类在 infrastructure/rpc 中。</p>
 *
 * <p><b>为什么需要这个抽象？</b>
 * <ul>
 *   <li>应用层不感知 Feign，便于单元测试（Mock 该接口即可）。</li>
 *   <li>未来切换为 gRPC / 本地缓存都不影响业务代码。</li>
 * </ul>
 */
public interface UserRolePermissionGateway {

    /**
     * 拉取用户的角色码集合。
     *
     * @param userId 用户 ID
     * @return 角色码，例如 ["USER"]；失败返回空集合
     */
    Set<String> fetchRoles(String userId);

    /**
     * 拉取用户的权限码集合。
     *
     * @param userId 用户 ID
     * @return 权限码，例如 ["order:create"]；失败返回空集合
     */
    Set<String> fetchPermissions(String userId);
}
