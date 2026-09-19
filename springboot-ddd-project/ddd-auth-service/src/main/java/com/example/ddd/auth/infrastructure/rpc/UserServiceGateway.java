package com.example.ddd.auth.infrastructure.rpc;

import com.example.ddd.auth.application.port.UserRolePermissionGateway;
import com.example.ddd.common.result.Result;
import com.example.ddd.contract.user.UserFeignClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

/**
 * Feign 适配器：调用 user-service 拉取角色权限。
 *
 * <p><b>降级策略：</b>
 * user-service 不可用时，返回空集合而不是抛异常，让登录流程可以继续。
 * 生产环境应该：
 * <ul>
 *   <li>加本地缓存 (Caffeine) 短期兜底。</li>
 *   <li>加熔断 (Resilience4j) 避免拖垮 auth 服务。</li>
 *   <li>关键权限（如支付、下单）拒绝空权限登录。</li>
 * </ul>
 */
@Component
public class UserServiceGateway implements UserRolePermissionGateway {

    private static final Logger log = LoggerFactory.getLogger(UserServiceGateway.class);

    private final UserFeignClient userFeignClient;

    public UserServiceGateway(@Autowired(required = false) UserFeignClient userFeignClient) {
        this.userFeignClient = userFeignClient;
    }

    @Override
    public Set<String> fetchRoles(String userId) {
        if (userFeignClient == null) {
            log.warn("[Feign-DRY-RUN] UserFeignClient unavailable, returning empty roles");
            return Collections.emptySet();
        }
        try {
            Result<Set<String>> result = userFeignClient.getRoleCodes(userId);
            if (result != null && result.isSuccess() && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("[Feign] fetchRoles failed userId={}: {}", userId, e.getMessage());
        }
        return Collections.emptySet();
    }

    @Override
    public Set<String> fetchPermissions(String userId) {
        if (userFeignClient == null) {
            log.warn("[Feign-DRY-RUN] UserFeignClient unavailable, returning empty permissions");
            return Collections.emptySet();
        }
        try {
            Result<Set<String>> result = userFeignClient.getPermissionCodes(userId);
            if (result != null && result.isSuccess() && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("[Feign] fetchPermissions failed userId={}: {}", userId, e.getMessage());
        }
        return Collections.emptySet();
    }
}
