package com.example.active7.init;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.active7.mapper.LeaveBalanceMapper;
import com.example.active7.mapper.SysPermissionMapper;
import com.example.active7.mapper.SysRoleMapper;
import com.example.active7.mapper.SysRolePermissionMapper;
import com.example.active7.mapper.SysUserMapper;
import com.example.active7.mapper.SysUserRoleMapper;
import com.example.active7.model.LeaveBalance;
import com.example.active7.model.SysPermission;
import com.example.active7.model.SysRole;
import com.example.active7.model.SysRolePermission;
import com.example.active7.model.SysUser;
import com.example.active7.model.SysUserRole;
import com.example.active7.security.RbacPermissions;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DemoDataInitializer implements ApplicationRunner {

    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final LeaveBalanceMapper leaveBalanceMapper;
    private final PasswordEncoder passwordEncoder;

    public DemoDataInitializer(SysRoleMapper sysRoleMapper,
                               SysPermissionMapper sysPermissionMapper,
                               SysRolePermissionMapper sysRolePermissionMapper,
                               SysUserMapper sysUserMapper,
                               SysUserRoleMapper sysUserRoleMapper,
                               LeaveBalanceMapper leaveBalanceMapper,
                               PasswordEncoder passwordEncoder) {
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
        this.sysRolePermissionMapper = sysRolePermissionMapper;
        this.sysUserMapper = sysUserMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.leaveBalanceMapper = leaveBalanceMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean firstRun = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()) == 0;
        initRoles();
        initPermissions();
        if (firstRun) {
            initRolePermissions();
        }
        initUsers();
        if (firstRun) {
            initUserRoles();
        }
        initBalances();
    }

    private void initRoles() {
        initRole("EMPLOYEE", "普通员工", "提交请假、查看自己的申请和待办");
        initRole("MANAGER", "部门经理", "审批经理任务，可以退回补充");
        initRole("HR", "人力资源", "审批长假、病假、婚假、丧假");
        initRole("DIRECTOR", "总监", "审批 5 天及以上请假");
        initRole("ADMIN", "系统管理员", "管理流程和 RBAC 权限");
    }

    private void initRole(String code, String name, String description) {
        Long count = sysRoleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, code));
        if (count == 0) {
            SysRole role = new SysRole();
            role.setRoleCode(code);
            role.setRoleName(name);
            role.setDescription(description);
            sysRoleMapper.insert(role);
        }
    }

    private void initPermissions() {
        initPermission(RbacPermissions.AUTH_VIEW, "查看当前身份", "API", "/api/auth/me", "读取登录用户、角色和权限");
        initPermission(RbacPermissions.LEAVE_CREATE, "提交请假", "API", "/api/leave/requests", "创建请假申请");
        initPermission(RbacPermissions.LEAVE_VIEW_MINE, "查看我的请假", "API", "/api/leave/requests/mine", "查看自己的申请和轨迹");
        initPermission(RbacPermissions.LEAVE_VIEW_ALL, "查看全部请假", "API", "/api/leave/requests", "管理员查看业务申请和轨迹");
        initPermission(RbacPermissions.LEAVE_VIEW_BALANCE, "查看年假余额", "API", "/api/leave/balance", "查看自己的年假余额");
        initPermission(RbacPermissions.LEAVE_WITHDRAW, "撤回请假", "API", "/api/leave/requests/withdraw", "撤回符合规则的申请");
        initPermission(RbacPermissions.LEAVE_TASK_VIEW, "查看我的待办", "API", "/api/leave/tasks/mine", "查询候选或已签收任务");
        initPermission(RbacPermissions.LEAVE_TASK_CLAIM, "签收任务", "API", "/api/leave/tasks/claim", "签收候选任务");
        initPermission(RbacPermissions.LEAVE_TASK_APPROVE, "办理审批", "API", "/api/leave/tasks/approve", "同意、拒绝、退回或确认");
        initPermission(RbacPermissions.LEAVE_TASK_RESUBMIT, "补充并重交", "API", "/api/leave/tasks/resubmit", "修改退回的申请");
        initPermission(RbacPermissions.PROCESS_VIEW, "查看流程管理", "MENU", "/active/index#admin", "查看部署、定义、XML 和运行时任务");
        initPermission(RbacPermissions.PROCESS_DEPLOY, "部署流程", "API", "/api/admin/deploy/zip", "上传 BPMN ZIP/BAR");
        initPermission(RbacPermissions.RBAC_VIEW, "查看 RBAC 模型", "API", "/api/admin/rbac", "查看用户、角色和权限关系");
        initPermission(RbacPermissions.RBAC_MANAGE, "管理 RBAC 关系", "API", "/api/admin/rbac", "分配用户角色和角色权限");
    }

    private void initPermission(String code, String name, String type, String resource, String description) {
        Long count = sysPermissionMapper.selectCount(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getPermissionCode, code));
        if (count == 0) {
            SysPermission permission = new SysPermission();
            permission.setPermissionCode(code);
            permission.setPermissionName(name);
            permission.setPermissionType(type);
            permission.setResource(resource);
            permission.setDescription(description);
            sysPermissionMapper.insert(permission);
        }
    }

    private void initRolePermissions() {
        String[] employeePermissions = new String[]{
                RbacPermissions.AUTH_VIEW,
                RbacPermissions.LEAVE_CREATE,
                RbacPermissions.LEAVE_VIEW_MINE,
                RbacPermissions.LEAVE_VIEW_BALANCE,
                RbacPermissions.LEAVE_WITHDRAW,
                RbacPermissions.LEAVE_TASK_VIEW,
                RbacPermissions.LEAVE_TASK_APPROVE,
                RbacPermissions.LEAVE_TASK_RESUBMIT
        };
        grantPermissions("EMPLOYEE", employeePermissions);
        grantPermissions("MANAGER", employeePermissions);
        grantPermissions("MANAGER", RbacPermissions.LEAVE_TASK_CLAIM);
        grantPermissions("HR", employeePermissions);
        grantPermissions("HR", RbacPermissions.LEAVE_TASK_CLAIM);
        grantPermissions("DIRECTOR", employeePermissions);
        grantPermissions("DIRECTOR", RbacPermissions.LEAVE_TASK_CLAIM);
        grantPermissions("ADMIN", RbacPermissions.AUTH_VIEW,
                RbacPermissions.LEAVE_VIEW_ALL,
                RbacPermissions.PROCESS_VIEW,
                RbacPermissions.PROCESS_DEPLOY,
                RbacPermissions.RBAC_VIEW,
                RbacPermissions.RBAC_MANAGE);
    }

    private void grantPermissions(String roleCode, String... permissionCodes) {
        for (String permissionCode : permissionCodes) {
            Long count = sysRolePermissionMapper.selectCount(new LambdaQueryWrapper<SysRolePermission>()
                    .eq(SysRolePermission::getRoleCode, roleCode)
                    .eq(SysRolePermission::getPermissionCode, permissionCode));
            if (count == 0) {
                SysRolePermission relation = new SysRolePermission();
                relation.setRoleCode(roleCode);
                relation.setPermissionCode(permissionCode);
                sysRolePermissionMapper.insert(relation);
            }
        }
    }

    private Map<String, String[]> demoUsers() {
        Map<String, String[]> users = new LinkedHashMap<String, String[]>();
        users.put("zhangsan", new String[]{"1001", "张三", "销售部", "EMPLOYEE", "123456"});
        users.put("lisi", new String[]{"1002", "李四", "销售部", "EMPLOYEE", "123456"});
        users.put("wangwu", new String[]{"1003", "王五", "销售部", "MANAGER", "123456"});
        users.put("zhaoliu", new String[]{"1004", "赵六", "人力资源部", "HR", "123456"});
        users.put("sunqi", new String[]{"1005", "孙七", "公司管理层", "DIRECTOR", "123456"});
        users.put("admin", new String[]{"9001", "管理员", "信息技术部", "ADMIN", "admin123"});
        return users;
    }

    private void initUsers() {
        for (Map.Entry<String, String[]> entry : demoUsers().entrySet()) {
            String username = entry.getKey();
            String[] values = entry.getValue();
            String userId = values[0];
            String displayName = values[1];
            String department = values[2];
            String rawPassword = values[4];

            Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, username));
            if (count == 0) {
                SysUser user = new SysUser();
                user.setUserId(userId);
                user.setUsername(username);
                user.setPassword(passwordEncoder.encode(rawPassword));
                user.setDisplayName(displayName);
                user.setDepartment(department);
                user.setEnabled(true);
                sysUserMapper.insert(user);
            }
        }
    }

    private void initUserRoles() {
        for (Map.Entry<String, String[]> entry : demoUsers().entrySet()) {
            String[] values = entry.getValue();
            String userId = values[0];
            String roleCode = values[3];
            Long roleCount = sysUserRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getUserId, userId)
                    .eq(SysUserRole::getRoleCode, roleCode));
            if (roleCount == 0) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(userId);
                userRole.setRoleCode(roleCode);
                sysUserRoleMapper.insert(userRole);
            }
        }
    }

    private void initBalances() {
        int year = LocalDate.now().getYear();
        String[] employeeIds = new String[]{"1001", "1002", "1003", "1004", "1005", "9001"};
        for (String userId : employeeIds) {
            Long count = leaveBalanceMapper.selectCount(new LambdaQueryWrapper<LeaveBalance>()
                    .eq(LeaveBalance::getUserId, userId)
                    .eq(LeaveBalance::getBalanceYear, year));
            if (count == 0) {
                LeaveBalance balance = new LeaveBalance();
                balance.setUserId(userId);
                balance.setBalanceYear(year);
                balance.setAnnualDays(new BigDecimal("10.0"));
                balance.setUsedDays(BigDecimal.ZERO);
                leaveBalanceMapper.insert(balance);
            }
        }
    }
}
