package com.example.dynamic.jpa.system.controller;


import com.example.common.result.ResultBody;
import com.example.dynamic.jpa.common.constants.SystemConstant;
import com.example.dynamic.jpa.common.util.JwtUtil;
import com.example.dynamic.jpa.security.token.TokenService;
import com.example.dynamic.jpa.security.JwtUser;
import com.example.dynamic.jpa.system.entity.Role;
import com.example.dynamic.jpa.system.service.RoleService;
import com.example.dynamic.jpa.system.vo.AddUserReqVo;
import com.example.dynamic.jpa.system.vo.RefreshReqVo;
import com.example.dynamic.jpa.system.service.TenantService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import com.example.dynamic.jpa.security.LoginUser;
import com.example.dynamic.jpa.system.config.MyDataSource;
import com.example.dynamic.jpa.system.entity.DataSourceType;
import com.example.dynamic.jpa.system.entity.User;
import com.example.dynamic.jpa.system.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
@MyDataSource(type = DataSourceType.SYSTEM)
public class SystemController {

    @Resource
    public UserService userService;

    @Resource
    private AuthenticationManager authenticationManager;

    @Resource
    private TokenService tokenService;

    @Resource
    private TenantService tenantService;

    @Resource
    private RoleService roleService;


    //添加租户账号
    @PostMapping(value = "/addTenantInfo")
    public ResultBody addTenantInfo(@jakarta.validation.Valid @RequestBody AddUserReqVo reqVo) {
        JwtUser principal = currentPrincipal();
        Integer currentTenant = principal.getUser().getTenantId();
        Integer targetTenant = reqVo.getTenantId();
        if (currentTenant == null
                || (currentTenant != SystemConstant.SYSTEM_TENANT_ID && !currentTenant.equals(targetTenant))) {
            throw new org.springframework.security.access.AccessDeniedException("不能为其他租户创建账号");
        }
        if (targetTenant > SystemConstant.SYSTEM_TENANT_ID && tenantService.getTenantById(targetTenant) == null) {
            throw new IllegalArgumentException("租户不存在");
        }
        Role role = roleService.getRoleById(reqVo.getRoleId());
        if (role == null) {
            throw new IllegalArgumentException("角色不存在或已停用");
        }
        User user = new User();
        user.setUsername(reqVo.getUsername());
        user.setNickname(org.apache.commons.lang3.StringUtils.defaultIfBlank(reqVo.getNickname(), reqVo.getUsername()));
        user.setPassword(reqVo.getPassword());
        user.setPhone(reqVo.getPhone());
        user.setEmail(reqVo.getEmail());
        user.setRoleId(role.getId());
        user.setTenantId(targetTenant);
        user.setType(reqVo.getType());
        userService.addUser(user);
        return ResultBody.success("用户注册成功！");
    }

    //用户登录：签发 access + refresh 双 token
    @PostMapping(value = "/login")
    public ResultBody login(@jakarta.validation.Valid @RequestBody LoginUser loginUser) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(loginUser.getUsername(), loginUser.getPassword()));
        JwtUser principal = (JwtUser) authentication.getPrincipal();
        if (loginUser.getTenantid() != null
                && !loginUser.getTenantid().equals(principal.getUser().getTenantId())) {
            throw new org.springframework.security.authentication.BadCredentialsException("账号或租户信息错误");
        }
        return ResultBody.success(tokenService.issueTokenPair(principal));
    }

    //用 refresh token 换取新的双 token（含 rotation，旧 refresh token 作废）
    @PostMapping(value = "/refresh")
    public ResultBody refresh(@jakarta.validation.Valid @RequestBody RefreshReqVo reqVo) {
        return ResultBody.success(tokenService.refresh(reqVo.getRefreshToken()));
    }

    //登出：拉黑当前 access token 并清空该用户 refresh token
    @PostMapping(value = "/logout")
    public ResultBody logout(@RequestHeader(value = JwtUtil.TOKEN_HEADER, required = false) String authHeader) {
        tokenService.logout(authHeader, currentPrincipal());
        return ResultBody.success("登出成功");
    }

    private JwtUser currentPrincipal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUser principal)) {
            throw new org.springframework.security.access.AccessDeniedException("登录状态无效");
        }
        return principal;
    }
}
