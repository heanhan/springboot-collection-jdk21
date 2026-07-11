package com.example.oauth.controller;

import com.example.oauth.common.result.ResultBody;
import com.example.oauth.entity.SysMenu;
import com.example.oauth.service.SysMenuAdminService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理控制器（RBAC 后台管理）
 */
@RestController
@RequestMapping("/admin/menus")
@Api(tags = "菜单管理接口")
public class SysMenuAdminController {

    private final SysMenuAdminService menuAdminService;

    public SysMenuAdminController(SysMenuAdminService menuAdminService) {
        this.menuAdminService = menuAdminService;
    }

    @GetMapping
    @ApiOperation("查询全部菜单（平铺）")
    public ResultBody<List<SysMenu>> listAll() {
        return ResultBody.success(menuAdminService.listAll());
    }

    @GetMapping("/tree")
    @ApiOperation("查询菜单树")
    public ResultBody<List<SysMenu>> tree() {
        return ResultBody.success(menuAdminService.tree());
    }

    @GetMapping("/{id}")
    @ApiOperation("查询菜单详情")
    public ResultBody<SysMenu> getById(@PathVariable Long id) {
        return ResultBody.success(menuAdminService.getById(id));
    }

    @PostMapping
    @ApiOperation("新增菜单")
    public ResultBody<SysMenu> create(@RequestBody SysMenu menu) {
        return ResultBody.success(menuAdminService.create(menu));
    }

    @PutMapping
    @ApiOperation("更新菜单")
    public ResultBody<SysMenu> update(@RequestBody SysMenu menu) {
        return ResultBody.success(menuAdminService.update(menu));
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除菜单")
    public ResultBody<Void> delete(@PathVariable Long id) {
        menuAdminService.delete(id);
        return ResultBody.success();
    }
}
