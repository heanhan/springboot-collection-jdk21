package com.example.active7.service.rbac;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.active7.mapper.SysAuthorityMapper;
import com.example.active7.mapper.SysPermissionMapper;
import com.example.active7.mapper.SysRoleMapper;
import com.example.active7.mapper.SysRolePermissionMapper;
import com.example.active7.mapper.SysUserMapper;
import com.example.active7.mapper.SysUserRoleMapper;
import com.example.active7.model.SysPermission;
import com.example.active7.model.SysRole;
import com.example.active7.model.SysRolePermission;
import com.example.active7.model.SysUser;
import com.example.active7.model.SysUserRole;
import com.example.active7.security.RbacPermissions;
import com.example.common.exceptins.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RbacService {

    private final SysAuthorityMapper sysAuthorityMapper;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;

    public RbacService(SysAuthorityMapper sysAuthorityMapper,
                       SysUserMapper sysUserMapper,
                       SysRoleMapper sysRoleMapper,
                       SysPermissionMapper sysPermissionMapper,
                       SysUserRoleMapper sysUserRoleMapper,
                       SysRolePermissionMapper sysRolePermissionMapper) {
        this.sysAuthorityMapper = sysAuthorityMapper;
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysRolePermissionMapper = sysRolePermissionMapper;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listUsers() {
        Map<String, Map<String, Object>> users = new LinkedHashMap<String, Map<String, Object>>();
        for (SysAuthorityRow row : sysAuthorityMapper.selectUserRoleRows()) {
            Map<String, Object> user = users.get(row.getUserId());
            if (user == null) {
                user = new LinkedHashMap<String, Object>();
                user.put("userId", row.getUserId());
                user.put("username", row.getUsername());
                user.put("displayName", row.getDisplayName());
                user.put("department", row.getDepartment());
                user.put("roles", new ArrayList<String>());
                users.put(row.getUserId(), user);
            }
            if (row.getRoleCode() != null) {
                addUnique((List<String>) user.get("roles"), row.getRoleCode());
            }
        }
        return new ArrayList<Map<String, Object>>(users.values());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listRoles() {
        Map<String, Map<String, Object>> roles = new LinkedHashMap<String, Map<String, Object>>();
        for (SysAuthorityRow row : sysAuthorityMapper.selectRolePermissionRows()) {
            Map<String, Object> role = roles.get(row.getRoleCode());
            if (role == null) {
                role = new LinkedHashMap<String, Object>();
                role.put("roleCode", row.getRoleCode());
                role.put("roleName", row.getRoleName());
                role.put("description", row.getRoleDescription());
                role.put("permissions", new ArrayList<String>());
                roles.put(row.getRoleCode(), role);
            }
            if (row.getPermissionCode() != null) {
                addUnique((List<String>) role.get("permissions"), row.getPermissionCode());
            }
        }
        return new ArrayList<Map<String, Object>>(roles.values());
    }

    @Transactional(readOnly = true)
    public List<SysPermission> listPermissions() {
        return sysPermissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .orderByAsc(SysPermission::getPermissionCode));
    }

    @Transactional
    public void replaceUserRoles(String userId, List<String> roleCodes) {
        lockAdminRole();
        if (sysUserMapper.selectById(userId) == null) {
            throw new BizException(404, "用户不存在");
        }
        Set<String> normalized = normalize(roleCodes);
        for (String roleCode : normalized) {
            Long count = sysRoleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getRoleCode, roleCode));
            if (count == 0) {
                throw new BizException(400, "角色不存在：" + roleCode);
            }
        }
        Long currentAdmin = sysUserRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getRoleCode, "ADMIN"));
        if (currentAdmin > 0 && !normalized.contains("ADMIN") && sysAuthorityMapper.countAdminUsers() <= 1) {
            throw new BizException(400, "系统至少需要保留一个管理员");
        }

        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
        for (String roleCode : normalized) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleCode(roleCode);
            sysUserRoleMapper.insert(relation);
        }
    }

    @Transactional
    public void replaceRolePermissions(String roleCode, List<String> permissionCodes) {
        lockAdminRole();
        if (sysRoleMapper.selectById(roleCode) == null) {
            throw new BizException(404, "角色不存在");
        }
        Set<String> normalized = normalize(permissionCodes);
        for (String permissionCode : normalized) {
            Long count = sysPermissionMapper.selectCount(new LambdaQueryWrapper<SysPermission>()
                    .eq(SysPermission::getPermissionCode, permissionCode));
            if (count == 0) {
                throw new BizException(400, "权限不存在：" + permissionCode);
            }
        }
        if ("ADMIN".equals(roleCode)) {
            List<String> required = Arrays.asList(RbacPermissions.AUTH_VIEW,
                    RbacPermissions.LEAVE_VIEW_ALL, RbacPermissions.PROCESS_VIEW,
                    RbacPermissions.PROCESS_DEPLOY, RbacPermissions.RBAC_VIEW,
                    RbacPermissions.RBAC_MANAGE);
            if (!normalized.containsAll(required)) {
                throw new BizException(400, "管理员角色必须保留流程和 RBAC 管理权限");
            }
        }

        sysRolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleCode, roleCode));
        for (String permissionCode : normalized) {
            SysRolePermission relation = new SysRolePermission();
            relation.setRoleCode(roleCode);
            relation.setPermissionCode(permissionCode);
            sysRolePermissionMapper.insert(relation);
        }
    }

    private void lockAdminRole() {
        sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, "ADMIN")
                .last("FOR UPDATE"));
    }

    private Set<String> normalize(List<String> values) {
        Set<String> result = new LinkedHashSet<String>();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.trim().isEmpty()) {
                    result.add(value.trim());
                }
            }
        }
        return result;
    }

    private void addUnique(List<String> target, String value) {
        if (!target.contains(value)) {
            target.add(value);
        }
    }
}
