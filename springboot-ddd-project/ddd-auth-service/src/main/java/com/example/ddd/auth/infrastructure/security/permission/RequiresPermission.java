package com.example.ddd.auth.infrastructure.security.permission;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自研方法级权限注解（<b>替代 Spring Security 的 {@code @PreAuthorize}</b>）。
 *
 * <p><b>为什么要自己写而不用 Spring Security 内置注解？</b>
 * <ol>
 *   <li>Spring Security 的 {@code @PreAuthorize("hasAuthority('order:create')")} 依赖 SpEL，
 *       表达能力强但也带来了运行期解析开销与"字符串魔法"，学习曲线陡峭。</li>
 *   <li>本项目希望权限校验逻辑<b>完全透明</b>：一个注解 + 一个 AOP 切面，
 *       代码路径清晰可 Debug，便于学习者理解认证/授权全流程。</li>
 *   <li>业务错误码统一走 {@link com.example.ddd.common.exception.BusinessException}，
 *       便于前端识别与国际化。</li>
 * </ol>
 *
 * <p><b>使用示例：</b>
 * <pre>
 * &#64;RequiresPermission("order:create")
 * public Order placeOrder(...) { ... }
 *
 * &#64;RequiresPermission(value = {"order:read", "order:export"}, logical = Logical.ALL)
 * public List&lt;Order&gt; exportOrders(...) { ... }
 * </pre>
 *
 * <p><b>校验流程：</b>由 {@link PermissionAspect} 拦截，从 {@code SecurityContext}
 * 取出 {@link com.example.ddd.auth.infrastructure.security.LoginUserPrincipal#permissions()}
 * 与本注解 {@link #value()} 做集合匹配。</p>
 *
 * @see PermissionAspect
 * @see RequiresRole
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {

    /**
     * 需要的权限码集合。
     * <p>例如 {@code "order:create"}、{@code "user:read"}。</p>
     */
    String[] value();

    /**
     * 多权限之间的逻辑关系。
     */
    Logical logical() default Logical.ANY;

    /**
     * 权限组合逻辑。
     */
    enum Logical {
        /** 任意一个满足即可（默认，OR 关系） */
        ANY,
        /** 必须全部满足（AND 关系） */
        ALL
    }
}
