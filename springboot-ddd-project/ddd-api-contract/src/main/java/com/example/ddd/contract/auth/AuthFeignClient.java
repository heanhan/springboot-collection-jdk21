package com.example.ddd.contract.auth;

import com.example.ddd.common.result.Result;
import com.example.ddd.contract.ServiceNames;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 认证服务对外 Feign 契约。
 *
 * <p><b>使用场景：</b>
 * 其他服务需要"从 AccessToken 中解析当前用户"时，通过本 Feign Client 调用 auth 服务。
 * 例如 order-service 收到用户下单请求，需要知道 userId，就可以调用 {@link #parseToken(String)}。</p>
 *
 * <p><b>为什么不用 JWT 库本地解析？</b>
 * 各服务都持有 JWT 密钥会带来密钥扩散、轮换困难等问题。集中让 auth 服务解析，
 * 其他服务只信任 auth 的返回结果，是典型的"认证即服务"设计。</p>
 *
 * <p><b>生产优化：</b>
 * 高频调用可加本地缓存 (Caffeine, TTL 30s)，或在网关层统一解析后透传 X-User-Id Header。</p>
 *
 * @author ddd-learning
 */
@FeignClient(name = ServiceNames.AUTH,
        contextId = "authFeignClient",
        path = "/auth/internal",
        url = "${ddd.services.auth-url:}")
public interface AuthFeignClient {

    /**
     * 解析 AccessToken，返回当前登录用户信息。
     *
     * @param token 完整的 Authorization Header 值，可以带 "Bearer " 前缀，也可以不带
     * @return 用户信息 Result；Token 无效时返回失败 Result
     */
    @GetMapping("/token/parse")
    Result<CurrentUserDTO> parseToken(@RequestHeader("Authorization") String token);

    /**
     * 根据 userId 查询用户的权限码集合（供 AOP 权限校验使用）。
     *
     * @param userId 用户 ID
     * @return 权限码集合，例如 ["order:create", "order:read"]
     */
    @GetMapping("/user/permissions")
    Result<java.util.Set<String>> getUserPermissions(@RequestHeader("X-User-Id") String userId);
}
