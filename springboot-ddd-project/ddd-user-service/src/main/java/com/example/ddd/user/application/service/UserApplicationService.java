package com.example.ddd.user.application.service;

import com.example.ddd.common.domain.event.DomainEvent;
import com.example.ddd.common.domain.model.BaseAggregateRoot;
import com.example.ddd.common.domain.valueobject.Email;
import com.example.ddd.common.domain.valueobject.Mobile;
import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;
import com.example.ddd.user.application.command.AssignRoleCommand;
import com.example.ddd.user.application.command.CreateUserCommand;
import com.example.ddd.user.application.command.UpdateProfileCommand;
import com.example.ddd.user.application.port.DomainEventPublisher;
import com.example.ddd.user.domain.model.aggregate.Role;
import com.example.ddd.user.domain.model.aggregate.User;
import com.example.ddd.user.domain.model.valueobject.Gender;
import com.example.ddd.user.domain.repository.RoleRepository;
import com.example.ddd.user.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 应用服务 (Application Service)：用户中心 - 用户资料用例。
 *
 * <p><b>用例列表：</b>
 * <ul>
 *   <li>{@link #createUser(CreateUserCommand)}：注册新用户（由 auth-service MQ 触发或直接调用）</li>
 *   <li>{@link #updateProfile(UpdateProfileCommand)}：修改资料</li>
 *   <li>{@link #disableUser(String, String)}：停用账号</li>
 *   <li>{@link #assignRole(AssignRoleCommand)}：分配 / 移除角色</li>
 *   <li>{@link #getUser(String)}：查询用户</li>
 * </ul>
 *
 * <p><b>事务边界：</b>
 * 每个 public 方法都是一个用例的事务边界。领域方法执行完毕后，
 * {@link #publishEvents(BaseAggregateRoot)} 负责把聚合根暂存的事件发布出去。</p>
 *
 * <p><b>为什么应用服务是"薄"的？</b>
 * 应用服务只做"编排"（找到聚合 -> 调用业务方法 -> 持久化 -> 发布事件），
 * 真正的业务规则都在领域对象里。这样应用服务几乎不包含 if/else 业务分支，
 * 变更频率低、易测试。</p>
 *
 * @author ddd-learning
 */
@Service
public class UserApplicationService {

    private static final Logger log = LoggerFactory.getLogger(UserApplicationService.class);

    /** 默认角色码：普通用户 */
    private static final String DEFAULT_ROLE_CODE = "USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DomainEventPublisher eventPublisher;

    public UserApplicationService(UserRepository userRepository,
                                  RoleRepository roleRepository,
                                  DomainEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 用例：创建用户。
     *
     * <p><b>编排步骤：</b>
     * <ol>
     *   <li>手机号唯一性校验（业务规则）。</li>
     *   <li>调用 {@link User#register} 工厂方法（领域行为，产生 UserRegisteredEvent）。</li>
     *   <li>为新用户分配默认角色 USER。</li>
     *   <li>持久化聚合。</li>
     *   <li>发布事件。</li>
     * </ol>
     */
    @Transactional
    public String createUser(CreateUserCommand cmd) {
        if (userRepository.existsByMobile(cmd.mobile())) {
            throw new BusinessException(ErrorCode.USER_MOBILE_DUPLICATE,
                    "手机号已被使用：" + cmd.mobile());
        }

        Mobile mobile = new Mobile(cmd.mobile());
        Email email = (cmd.email() == null || cmd.email().isBlank()) ? null : new Email(cmd.email());
        Gender gender = Gender.fromCode(cmd.gender());

        User user = User.register(cmd.nickname(), mobile, email, gender);

        // 分配默认角色（跨聚合协作，只保存 roleId）
        roleRepository.findByCode(DEFAULT_ROLE_CODE).ifPresent(role -> user.assignRole(role.getRoleId()));

        userRepository.save(user);
        publishEvents(user);
        log.info("[User] created userId={} mobile={}", user.getUserId(), mobile.masked());
        return user.getUserId();
    }

    /**
     * 用例：修改资料。
     */
    @Transactional
    public void updateProfile(UpdateProfileCommand cmd) {
        User user = loadUser(cmd.userId());
        Email email = (cmd.email() == null || cmd.email().isBlank()) ? null : new Email(cmd.email());
        user.updateProfile(cmd.nickname(), cmd.avatar(), cmd.gender() == null ? null : Gender.fromCode(cmd.gender()));
        if (email != null) {
            user.changeEmail(email);
        }
        userRepository.save(user);
        publishEvents(user);
    }

    /**
     * 用例：停用账号。
     */
    @Transactional
    public void disableUser(String userId, String reason) {
        User user = loadUser(userId);
        user.disable(reason);
        userRepository.save(user);
        publishEvents(user);
    }

    /**
     * 用例：启用账号。
     */
    @Transactional
    public void enableUser(String userId) {
        User user = loadUser(userId);
        user.enable();
        userRepository.save(user);
    }

    /**
     * 用例：分配 / 移除角色。
     */
    @Transactional
    public void assignRole(AssignRoleCommand cmd) {
        User user = loadUser(cmd.userId());
        Role role = roleRepository.findById(cmd.roleId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_ROLE_NOT_FOUND,
                        "角色不存在：" + cmd.roleId()));
        if (!role.isActive()) {
            throw new BusinessException(ErrorCode.CONFLICT, "角色已停用，不可分配");
        }
        if (cmd.assign()) {
            user.assignRole(role.getRoleId());
        } else {
            user.removeRole(role.getRoleId());
        }
        userRepository.save(user);
    }

    /**
     * 用例：查询用户。
     */
    @Transactional(readOnly = true)
    public User getUser(String userId) {
        return loadUser(userId);
    }

    /**
     * 用例：分页查询用户列表（管理端）。
     */
    @Transactional(readOnly = true)
    public List<User> listUsers(int pageNum, int pageSize) {
        return userRepository.list(pageNum, pageSize);
    }

    @Transactional(readOnly = true)
    public long countUsers() {
        return userRepository.count();
    }

    // ============================================================
    // 内部工具方法
    // ============================================================

    private User loadUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND,
                        "用户不存在：" + userId));
    }

    /**
     * 发布聚合根收集到的领域事件，并清空列表避免重复发布。
     */
    private void publishEvents(BaseAggregateRoot aggregate) {
        for (DomainEvent event : aggregate.getDomainEvents()) {
            try {
                eventPublisher.publish(event);
            } catch (Exception e) {
                // 事件发布失败不应该导致主事务回滚（除非业务明确要求）
                // 生产环境应引入本地事件表 (outbox pattern) 保证最终一致
                log.error("[User] publish event failed: {}", event, e);
            }
        }
        aggregate.clearDomainEvents();
    }
}
