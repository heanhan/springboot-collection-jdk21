package com.example.oauth.security;

import com.example.oauth.entity.SysPermission;
import com.example.oauth.repository.SysPermissionRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 动态权限缓存
 * <p>
 * 加载 sys_permission 中 type=3（接口权限）且 status=1（启用）的记录，
 * 构建「URL + HTTP 方法 -> 权限编码」映射规则表并缓存于内存。
 * 权限数据变更后（如管理接口新增/修改权限），调用 {@link #refresh()} 重新加载。
 */
@Slf4j
@Service
public class DynamicPermissionCache {

    private final SysPermissionRepository permissionRepository;

    /** 权限规则表（volatile 保证刷新后其他线程可见） */
    private volatile List<PermissionRule> rules = Collections.emptyList();

    public DynamicPermissionCache(SysPermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @PostConstruct
    public void refresh() {
        try {
            List<SysPermission> perms = permissionRepository.findByTypeAndStatus(3, 1);
            List<PermissionRule> list = new ArrayList<>();
            for (SysPermission p : perms) {
                if (!StringUtils.hasText(p.getUrl())) {
                    continue;
                }
                Set<String> methods = new HashSet<>();
                if (StringUtils.hasText(p.getMethod())) {
                    for (String m : p.getMethod().split(",")) {
                        if (StringUtils.hasText(m)) {
                            methods.add(m.trim().toUpperCase());
                        }
                    }
                }
                list.add(new PermissionRule(p.getUrl().trim(), methods, p.getPermCode()));
            }
            this.rules = list;
            log.info("动态权限规则已加载，共 {} 条接口权限", list.size());
        } catch (Exception e) {
            // 启动阶段数据库尚未就绪等异常不应中断应用；保留旧规则并告警
            log.error("加载动态权限规则失败: {}", e.getMessage());
        }
    }

    public List<PermissionRule> getRules() {
        return rules;
    }

    /**
     * 权限规则：一条接口权限对应的 URL 模式、允许的方法集合及所需权限编码
     */
    @Getter
    public static class PermissionRule {
        /** Ant 风格 URL 模式 */
        private final String urlPattern;
        /** 允许的 HTTP 方法（大写）；为空表示不限方法 */
        private final Set<String> methods;
        /** 访问该资源所需的权限编码 */
        private final String permCode;

        public PermissionRule(String urlPattern, Set<String> methods, String permCode) {
            this.urlPattern = urlPattern;
            this.methods = methods;
            this.permCode = permCode;
        }
    }
}
