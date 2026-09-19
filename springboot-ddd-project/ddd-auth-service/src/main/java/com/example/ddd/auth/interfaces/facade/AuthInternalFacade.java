package com.example.ddd.auth.interfaces.facade;

import com.example.ddd.auth.application.port.TokenProvider;
import com.example.ddd.auth.application.service.AuthApplicationService;
import com.example.ddd.common.result.Result;
import com.example.ddd.contract.auth.CurrentUserDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * Facade (对内接口)：实现 {@code com.example.ddd.contract.auth.AuthFeignClient} 的服务端点。
 *
 * <p><b>为什么单独一个 Facade？</b>
 * <ul>
 *   <li>URL 前缀 {@code /auth/internal/**} 明确表示"内部服务调用"，可在网关禁止外网访问。</li>
 *   <li>返回类型是<b>契约 DTO</b> ({@link CurrentUserDTO})，跨服务稳定。</li>
 *   <li>无需权限校验（内部服务通过网关白名单或 mTLS 保护）。</li>
 * </ul>
 *
 * <p><b>典型使用场景：</b>
 * order-service 收到用户下单请求，通过 {@code AuthFeignClient.parseToken(header)}
 * 拿到 userId，避免各服务重复持有 JWT 密钥。</p>
 *
 * <p><b>路径必须与契约接口的 {@code @GetMapping} 完全一致。</b></p>
 */
@RestController
@RequestMapping("/auth/internal")
public class AuthInternalFacade {

    private final AuthApplicationService authService;

    public AuthInternalFacade(AuthApplicationService authService) {
        this.authService = authService;
    }

    /**
     * 解析 AccessToken，返回当前登录用户信息。
     *
     * @param token 完整 Authorization Header 值，可以带 "Bearer " 前缀
     */
    @GetMapping("/token/parse")
    public Result<CurrentUserDTO> parseToken(@RequestHeader("Authorization") String token) {
        TokenProvider.AccessTokenPayload payload = authService.parseToken(token);
        if (payload == null) {
            return Result.fail("1006", "Token 无效或已过期");
        }
        CurrentUserDTO dto = new CurrentUserDTO(
                payload.userId(), payload.username(), payload.tokenId(),
                payload.roles(), payload.permissions(), payload.expiresAt());
        return Result.ok(dto);
    }

    /**
     * 根据 userId 查询用户的权限码集合。
     */
    @GetMapping("/user/permissions")
    public Result<Set<String>> getUserPermissions(@RequestHeader("X-User-Id") String userId) {
        return Result.ok(authService.getUserPermissions(userId));
    }
}
