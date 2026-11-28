package com.example.dynamic.jpa.system.controller;

import com.example.common.result.ResultBody;
import com.example.dynamic.jpa.security.JwtUser;
import com.example.dynamic.jpa.system.entity.AuthNode;
import com.example.dynamic.jpa.system.service.AuthNodeService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth-nodes")
public class PermissionController {

    @Resource
    private AuthNodeService authNodeService;

    @GetMapping
    public ResultBody list() {
        return ResultBody.success(authNodeService.listActiveAuthNodes());
    }

    @PostMapping
    public ResultBody add(@Valid @RequestBody AuthNode node) {
        requirePlatform();
        return ResultBody.success(authNodeService.addAuthNode(node));
    }

    @PutMapping("/{id}")
    public ResultBody update(@PathVariable Integer id, @Valid @RequestBody AuthNode node) {
        requirePlatform();
        node.setId(id);
        return ResultBody.success(authNodeService.editAuthNode(node));
    }

    @DeleteMapping("/{id}")
    public ResultBody delete(@PathVariable Integer id) {
        requirePlatform();
        return ResultBody.success(authNodeService.deleteAuthNode(id));
    }

    private void requirePlatform() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUser user)
                || user.getUser().getTenantId() != 0) {
            throw new org.springframework.security.access.AccessDeniedException("仅平台管理员可以修改权限节点");
        }
    }
}
