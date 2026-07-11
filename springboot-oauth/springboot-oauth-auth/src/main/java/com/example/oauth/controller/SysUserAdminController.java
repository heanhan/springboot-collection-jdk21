package com.example.oauth.controller;

import com.example.oauth.common.result.ResultBody;
import com.example.oauth.dto.AssignDTO;
import com.example.oauth.dto.UserSaveDTO;
import com.example.oauth.entity.SysUser;
import com.example.oauth.service.SysUserAdminService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 用户管理控制器（RBAC 后台管理）
 */
@RestController
@RequestMapping("/admin/users")
@Api(tags = "用户管理接口")
public class SysUserAdminController {

    private final SysUserAdminService userAdminService;

    public SysUserAdminController(SysUserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    @ApiOperation("分页查询用户")
    public ResultBody<Page<SysUser>> page(@RequestParam(required = false) String keyword,
                                          @RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "10") Integer pageSize) {
        Pageable pageable = PageRequest.of(Math.max(0, pageNum - 1), pageSize,
                Sort.by(Sort.Direction.DESC, "id"));
        return ResultBody.success(userAdminService.page(keyword, pageable));
    }

    @GetMapping("/{id}")
    @ApiOperation("查询用户详情")
    public ResultBody<SysUser> getById(@PathVariable Long id) {
        return ResultBody.success(userAdminService.getById(id));
    }

    @PostMapping
    @ApiOperation("新增用户")
    public ResultBody<SysUser> create(@Valid @RequestBody UserSaveDTO dto) {
        return ResultBody.success(userAdminService.create(dto));
    }

    @PutMapping
    @ApiOperation("更新用户")
    public ResultBody<SysUser> update(@RequestBody UserSaveDTO dto) {
        return ResultBody.success(userAdminService.update(dto));
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除用户")
    public ResultBody<Void> delete(@PathVariable Long id) {
        userAdminService.delete(id);
        return ResultBody.success();
    }

    @PutMapping("/{id}/status")
    @ApiOperation("修改用户状态")
    public ResultBody<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        userAdminService.changeStatus(id, status);
        return ResultBody.success();
    }

    @PutMapping("/{id}/password")
    @ApiOperation("重置用户密码")
    public ResultBody<Void> resetPassword(@PathVariable Long id, @RequestParam String password) {
        userAdminService.resetPassword(id, password);
        return ResultBody.success();
    }

    @PutMapping("/{id}/roles")
    @ApiOperation("给用户分配角色")
    public ResultBody<Void> assignRoles(@PathVariable Long id, @RequestBody AssignDTO dto) {
        userAdminService.assignRoles(id, dto.getIds());
        return ResultBody.success();
    }

    @GetMapping("/{id}/roles")
    @ApiOperation("查询用户已分配角色 ID")
    public ResultBody<List<Long>> getRoleIds(@PathVariable Long id) {
        return ResultBody.success(userAdminService.getRoleIds(id));
    }
}
