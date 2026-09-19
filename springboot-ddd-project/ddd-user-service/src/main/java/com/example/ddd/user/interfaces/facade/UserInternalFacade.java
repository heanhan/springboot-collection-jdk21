package com.example.ddd.user.interfaces.facade;

import com.example.ddd.common.result.Result;
import com.example.ddd.contract.user.AddressDTO;
import com.example.ddd.contract.user.UserDTO;
import com.example.ddd.user.application.service.AddressApplicationService;
import com.example.ddd.user.application.service.RbacQueryService;
import com.example.ddd.user.application.service.UserApplicationService;
import com.example.ddd.user.domain.model.aggregate.User;
import com.example.ddd.user.domain.model.entity.AddressEntry;
import com.example.ddd.user.interfaces.assembler.UserAssembler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * Facade (对内接口)：实现 {@code com.example.ddd.contract.user.UserFeignClient} 的服务端点。
 *
 * <p><b>为什么单独一个 Facade 类而不复用 UserController？</b>
 * <ul>
 *   <li>URL 前缀不同：{@code /user/internal/**} 明确表示"内部服务调用"，可以在网关层禁止外网访问。</li>
 *   <li>DTO 不同：内部返回<b>契约 DTO</b>（发布语言，跨服务稳定），外部返回<b>UI DTO</b>（可随前端需求变化）。</li>
 *   <li>权限不同：内部接口通常只需要服务间认证（mTLS 或 Header Token），不需要用户级权限。</li>
 * </ul>
 *
 * <p><b>路径映射：</b>
 * 必须与 {@link com.example.ddd.contract.user.UserFeignClient} 上的 {@code @GetMapping} 保持一致。</p>
 */
@RestController
@RequestMapping("/user/internal")
public class UserInternalFacade {

    private final UserApplicationService userApplicationService;
    private final AddressApplicationService addressApplicationService;
    private final RbacQueryService rbacQueryService;

    public UserInternalFacade(UserApplicationService userApplicationService,
                              AddressApplicationService addressApplicationService,
                              RbacQueryService rbacQueryService) {
        this.userApplicationService = userApplicationService;
        this.addressApplicationService = addressApplicationService;
        this.rbacQueryService = rbacQueryService;
    }

    @GetMapping("/{userId}")
    public Result<UserDTO> getById(@PathVariable("userId") String userId) {
        User user = userApplicationService.getUser(userId);
        return Result.ok(UserAssembler.toContractDTO(user));
    }

    @GetMapping("/{userId}/default-address")
    public Result<AddressDTO> getDefaultAddress(@PathVariable("userId") String userId) {
        AddressEntry entry = addressApplicationService.getDefaultAddress(userId);
        return Result.ok(UserAssembler.toContractDTO(entry));
    }

    @GetMapping("/{userId}/address/{addressId}")
    public Result<AddressDTO> getAddress(@PathVariable("userId") String userId,
                                         @PathVariable("addressId") String addressId) {
        AddressEntry entry = addressApplicationService.getAddress(userId, addressId);
        return Result.ok(UserAssembler.toContractDTO(entry));
    }

    @GetMapping("/{userId}/roles")
    public Result<Set<String>> getRoleCodes(@PathVariable("userId") String userId) {
        return Result.ok(rbacQueryService.findRoleCodesByUserId(userId));
    }

    @GetMapping("/{userId}/permissions")
    public Result<Set<String>> getPermissionCodes(@PathVariable("userId") String userId) {
        return Result.ok(rbacQueryService.findPermissionCodesByUserId(userId));
    }
}
