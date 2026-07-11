package com.example.oauth.controller;

import com.example.oauth.common.result.ResultBody;
import com.example.oauth.entity.SysPermission;
import com.example.oauth.service.SysPermissionAdminService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理控制器（RBAC 后台管理）
 */
@RestController
@RequestMapping("/admin/permissions")
@Api(tags = "权限管理接口")
public class SysPermissionAdminController {

    private final SysPermissionAdminService permissionAdminService;

    public SysPermissionAdminController(SysPermissionAdminService permissionAdminService) {
        this.permissionAdminService = permissionAdminService;
    }

    @GetMapping
    @ApiOperation("分页查询权限")
    public ResultBody<Page<SysPermission>> page(@RequestParam(required = false) String keyword,
                                                @RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "10") Integer pageSize) {
        Pageable pageable = PageRequest.of(Math.max(0, pageNum - 1), pageSize,
                Sort.by(Sort.Direction.DESC, "id"));
        return ResultBody.success(permissionAdminService.page(keyword, pageable));
    }

    @GetMapping("/all")
    @ApiOperation("查询全部权限")
    public ResultBody<List<SysPermission>> listAll() {
        return ResultBody.success(permissionAdminService.listAll());
    }

    @GetMapping("/{id}")
    @ApiOperation("查询权限详情")
    public ResultBody<SysPermission> getById(@PathVariable Long id) {
        return ResultBody.success(permissionAdminService.getById(id));
    }

    @PostMapping
    @ApiOperation("新增权限")
    public ResultBody<SysPermission> create(@RequestBody SysPermission permission) {
        return ResultBody.success(permissionAdminService.create(permission));
    }

    @PutMapping
    @ApiOperation("更新权限")
    public ResultBody<SysPermission> update(@RequestBody SysPermission permission) {
        return ResultBody.success(permissionAdminService.update(permission));
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除权限")
    public ResultBody<Void> delete(@PathVariable Long id) {
        permissionAdminService.delete(id);
        return ResultBody.success();
    }
}
