package com.example.dynamic.jpa.security;

import com.example.dynamic.jpa.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Collection;

/**
 * @Author : zhaojh
 * @Description : 自定义权限资源过滤器，实现动态的权限验证
 */
@Component
@Slf4j
public class MyFilterInvocationSecurityMetadataSource implements FilterInvocationSecurityMetadataSource {

//    @Resource
//    private RoleService roleService;

    private AntPathMatcher antPathMatcher = new AntPathMatcher();


    /**
     * 直接获取本次接口访问的用户权限（解析请求的token），
     * 然后获取用户可以访问的节点，匹配路径
     **/
    @Override
    public Collection<ConfigAttribute> getAttributes(Object o) {
        FilterInvocation filterInvocation = (FilterInvocation) o;
        HttpServletRequest request = filterInvocation.getRequest();
        // 从token中获取登录用户信息
        String token = request.getHeader(JwtUtil.TOKEN_HEADER);
//        Identity identity = IdentityUtils.getIdentityByToken(token);
        String requestUrl = filterInvocation.getRequestUrl();
        if(requestUrl.contains("/auth/login")){
            return null;
        }
//        if (identity != null && identity.getRoleId() != null) {
//            // 获取角色的所有权限
//            List<AuthNode> authNodeList = roleService.lisAuthNodeByRole(identity.getRoleId());
//            Role role = roleService.getRoleById(identity.getRoleId());
//            for (AuthNode authNode : authNodeList) {
//                if (antPathMatcher.match(authNode.getPath(), requestUrl)) {
//                    Collection<ConfigAttribute> configAttributes = new ArrayList<>();
//                    configAttributes.add(new SecurityConfig(role.getRoleName()));
//                    return configAttributes;
//                }
//            }
//        }
        //如果本方法返回null的话，意味着当前这个请求不需要任何角色就能访问
        //此处做逻辑控制，如果没有匹配上的，返回一个默认具体权限，防止漏缺资源配置
        return SecurityConfig.createList("ROLE_TENANT_ADMIN");
    }

    /**
     * 此处方法如果做了实现，返回了定义的权限资源列表，
     * Spring Security会在启动时校验每个ConfigAttribute是否配置正确，
     * 如果不需要校验，这里实现方法，方法体直接返回null即可。
     **/
    @Override
    public Collection<ConfigAttribute> getAllConfigAttributes() {
        return null;
    }

    /**
     * 方法返回类对象是否支持校验，
     * web项目一般使用FilterInvocation来判断，或者直接返回true
     **/
    @Override
    public boolean supports(Class<?> aClass) {
        return FilterInvocation.class.isAssignableFrom(aClass);
    }
}
