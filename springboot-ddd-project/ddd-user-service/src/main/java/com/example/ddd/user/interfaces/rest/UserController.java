package com.example.ddd.user.interfaces.rest;

import com.example.ddd.common.result.PageResult;
import com.example.ddd.common.result.Result;
import com.example.ddd.user.application.command.AssignRoleCommand;
import com.example.ddd.user.application.command.CreateUserCommand;
import com.example.ddd.user.application.command.UpdateProfileCommand;
import com.example.ddd.user.application.service.UserApplicationService;
import com.example.ddd.user.domain.model.aggregate.User;
import com.example.ddd.user.interfaces.dto.CreateUserRequest;
import com.example.ddd.user.interfaces.dto.UpdateProfileRequest;
import com.example.ddd.user.interfaces.dto.UserDetailResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST 控制器：用户资料管理（面向前端 / 管理端）。
 *
 * <p><b>DDD 中的角色：</b>
 * Controller 是<b>用户接口层 (User Interface Layer)</b> 的适配器，
 * 负责把 HTTP 请求翻译为对应用服务的调用。<b>禁止</b>在这里写业务逻辑。</p>
 *
 * <p><b>认证方式：</b>
 * user-service 本身不含 Spring Security，通过网关或前置 Filter 校验 Token 后
 * 把 userId 透传到 Header (X-User-Id)。本项目为了简化学习曲线，
 * 直接通过路径参数传 userId，实际部署时应该在网关做越权校验。</p>
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserApplicationService userApplicationService;

    public UserController(UserApplicationService userApplicationService) {
        this.userApplicationService = userApplicationService;
    }

    /**
     * 创建用户（管理员使用；普通用户注册走 auth-service）。
     */
    @PostMapping
    public Result<String> createUser(@Valid @RequestBody CreateUserRequest req) {
        String userId = userApplicationService.createUser(
                new CreateUserCommand(req.nickname(), req.mobile(), req.email(), req.gender()));
        return Result.ok(userId);
    }

    /**
     * 查询用户详情。
     */
    @GetMapping("/{userId}")
    public Result<UserDetailResponse> getUser(@PathVariable String userId) {
        User user = userApplicationService.getUser(userId);
        return Result.ok(UserDetailResponse.from(user));
    }

    /**
     * 修改用户资料。
     */
    @PutMapping("/{userId}")
    public Result<Void> updateProfile(@PathVariable String userId,
                                      @Valid @RequestBody UpdateProfileRequest req) {
        userApplicationService.updateProfile(
                new UpdateProfileCommand(userId, req.nickname(), req.avatar(), req.gender(), req.email()));
        return Result.ok();
    }

    /**
     * 停用用户。
     */
    @PostMapping("/{userId}/disable")
    public Result<Void> disable(@PathVariable String userId,
                                @RequestParam(required = false, defaultValue = "manual") String reason) {
        userApplicationService.disableUser(userId, reason);
        return Result.ok();
    }

    /**
     * 启用用户。
     */
    @PostMapping("/{userId}/enable")
    public Result<Void> enable(@PathVariable String userId) {
        userApplicationService.enableUser(userId);
        return Result.ok();
    }

    /**
     * 分配角色。
     */
    @PostMapping("/{userId}/roles/{roleId}")
    public Result<Void> assignRole(@PathVariable String userId, @PathVariable String roleId) {
        userApplicationService.assignRole(new AssignRoleCommand(userId, roleId, true));
        return Result.ok();
    }

    /**
     * 移除角色。
     */
    @DeleteMapping("/{userId}/roles/{roleId}")
    public Result<Void> removeRole(@PathVariable String userId, @PathVariable String roleId) {
        userApplicationService.assignRole(new AssignRoleCommand(userId, roleId, false));
        return Result.ok();
    }

    /**
     * 分页查询用户列表（管理端）。
     */
    @GetMapping
    public Result<PageResult<UserDetailResponse>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                       @RequestParam(defaultValue = "20") int pageSize) {
        List<User> users = userApplicationService.listUsers(pageNum, pageSize);
        long total = userApplicationService.countUsers();
        List<UserDetailResponse> items = users.stream().map(UserDetailResponse::from).toList();
        return Result.ok(new PageResult<>(items, total, pageNum, pageSize, 0));
    }
}
