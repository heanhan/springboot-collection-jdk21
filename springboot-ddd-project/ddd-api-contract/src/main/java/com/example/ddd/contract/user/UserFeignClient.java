package com.example.ddd.contract.user;

import com.example.ddd.common.result.Result;
import com.example.ddd.contract.ServiceNames;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 用户服务对外 Feign 契约。
 *
 * @author ddd-learning
 */
@FeignClient(name = ServiceNames.USER,
        contextId = "userFeignClient",
        path = "/user/internal",
        url = "${ddd.services.user-url:}")
public interface UserFeignClient {

    /**
     * 根据 userId 查询用户基本信息。
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    @GetMapping("/{userId}")
    Result<UserDTO> getById(@PathVariable("userId") String userId);

    /**
     * 根据 userId 查询默认收货地址。
     *
     * @param userId 用户 ID
     * @return 默认地址，未设置则返回 null（Result.data == null）
     */
    @GetMapping("/{userId}/default-address")
    Result<AddressDTO> getDefaultAddress(@PathVariable("userId") String userId);

    /**
     * 根据 addressId 查询地址详情。
     *
     * @param userId    用户 ID（用于越权校验）
     * @param addressId 地址 ID
     * @return 地址详情
     */
    @GetMapping("/{userId}/address/{addressId}")
    Result<AddressDTO> getAddress(@PathVariable("userId") String userId,
                                  @PathVariable("addressId") String addressId);

    /**
     * 查询用户的角色码集合，供 auth-service 登录时构造 JWT payload 使用。
     *
     * @param userId 用户 ID
     * @return 角色码集合，例如 ["USER"]
     */
    @GetMapping("/{userId}/roles")
    Result<java.util.Set<String>> getRoleCodes(@PathVariable("userId") String userId);

    /**
     * 查询用户的权限码集合，供 auth-service 登录时构造 JWT payload 使用。
     *
     * @param userId 用户 ID
     * @return 权限码集合，例如 ["order:create", "order:read"]
     */
    @GetMapping("/{userId}/permissions")
    Result<java.util.Set<String>> getPermissionCodes(@PathVariable("userId") String userId);
}
