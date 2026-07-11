package com.example.oauth.controller;

import com.example.oauth.common.result.ResultBody;
import com.example.oauth.dto.AssignDTO;
import com.example.oauth.entity.SysRole;
import com.example.oauth.service.SysRoleAdminService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器（RBAC 后台管理）
 */
@RestController
@RequestMapping("/admin/roles")
@Api(tags = "角色管理接口")
public class SysRoleAdminController {

    private final SysRoleAdminService roleAdminService;

    public SysRoleAdminController(SysRoleAdminService roleAdminService) {
        this.roleAdminService = roleAdminService;
    }

    @GetMapping
    @ApiOperation("分页查询角色")
    public ResultBody<Page<SysRole>> page(@RequestParam(required = false) String keyword,
                                          @RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "10") Integer pageSize) {
        Pageable pageable = PageRequest.of(Math.max(0, pageNum - 1), pageSize,
                Sort.by(Sort.Direction.DESC, "id"));
        return ResultBody.success(roleAdminService.page(keyword, pageable));
    }

    @GetMapping("/all")
    @ApiOperation("查询全部角色")
    public ResultBody<List<SysRole>> listAll() {
        return ResultBody.success(roleAdminService.listAll());
    }

    @GetMapping("/{id}")
    @ApiOperation("查询角色详情")
    public ResultBody<SysRole> getById(@PathVariable Long id) {
        return ResultBody.success(roleAdminService.getById(id));
    }

    @PostMapping
    @ApiOperation("新增角色")
    public ResultBody<SysRole> create(@RequestBody SysRole role) {
        return ResultBody.success(roleAdminService.create(role));
    }

    @PutMapping
    @ApiOperation("更新角色")
    public ResultBody<SysRole> update(@RequestBody SysRole role) {
        return ResultBody.success(roleAdminService.update(role));
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除角色")
    public ResultBody<Void> delete(@PathVariable Long id) {
        roleAdminService.delete(id);
        return ResultBody.success();
    }

    @PutMapping("/{id}/permissions")
    @ApiOperation("给角色分配权限")
    public ResultBody<Void> assignPermissions(@PathVariable Long id, @RequestBody AssignDTO dto) {
        roleAdminService.assignPermissions(id, dto.getIds());
        return ResultBody.success();
    }

    @GetMapping("/{id}/permissions")
    @ApiOperation("查询角色已分配权限 ID")
    public ResultBody<List<Long>> getPermissionIds(@PathVariable Long id) {
        return ResultBody.success(roleAdminService.getPermissionIds(id));
    }

    @PutMapping("/{id}/menus")
    @ApiOperation("给角色分配菜单")
    public ResultBody<Void> assignMenus(@PathVariable Long id, @RequestBody AssignDTO dto) {
        roleAdminService.assignMenus(id, dto.getIds());
        return ResultBody.success();
    }

    @GetMapping("/{id}/menus")
    @ApiOperation("查询角色已分配菜单 ID")
    public ResultBody<List<Long>> getMenuIds(@PathVariable Long id) {
        return ResultBody.success(roleAdminService.getMenuIds(id));
    }
}
