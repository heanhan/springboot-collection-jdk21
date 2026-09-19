package com.example.ddd.auth.application.service;

import com.example.ddd.auth.application.command.ChangePasswordCommand;
import com.example.ddd.auth.application.command.LoginCommand;
import com.example.ddd.auth.application.command.RegisterCommand;
import com.example.ddd.auth.application.dto.TokenPairDTO;
import com.example.ddd.auth.application.port.DomainEventPublisher;
import com.example.ddd.auth.application.port.TokenProvider;
import com.example.ddd.auth.application.port.TokenStore;
import com.example.ddd.auth.application.port.UserRolePermissionGateway;
import com.example.ddd.auth.domain.model.aggregate.UserCredential;
import com.example.ddd.auth.domain.repository.UserCredentialRepository;
import com.example.ddd.auth.domain.service.PasswordHasher;
import com.example.ddd.common.domain.event.DomainEvent;
import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;
import com.example.ddd.common.util.IdGenerator;
import com.example.ddd.contract.auth.event.AccountCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

/**
 * 应用服务：认证核心用例。
 *
 * <p><b>用例：</b>
 * <ul>
 *   <li>{@link #register(RegisterCommand)}：注册（凭据 + 发 AccountCreatedEvent）</li>
 *   <li>{@link #login(LoginCommand)}：登录（校验密码 + 签发双 Token + 缓存权限）</li>
 *   <li>{@link #refresh(String)}：刷新 Token（RefreshToken 换新的双 Token）</li>
 *   <li>{@link #logout(String, String)}：注销（AccessToken 拉黑 + 删 RefreshToken）</li>
 *   <li>{@link #changePassword(ChangePasswordCommand)}：修改密码</li>
 * </ul>
 *
 * <p><b>为什么把 Token 签发放在应用服务而不是领域服务？</b>
 * Token 签发涉及多个协作对象（Credential 聚合、TokenProvider、TokenStore、RoleGateway），
 * 属于"编排"职责，符合应用服务定位。领域对象只负责自己的状态与规则。</p>
 */
@Service
public class AuthApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AuthApplicationService.class);

    private final UserCredentialRepository credentialRepository;
    private final PasswordHasher passwordHasher;
    private final TokenProvider tokenProvider;
    private final TokenStore tokenStore;
    private final UserRolePermissionGateway roleGateway;
    private final DomainEventPublisher eventPublisher;

    public AuthApplicationService(UserCredentialRepository credentialRepository,
                                  PasswordHasher passwordHasher,
                                  TokenProvider tokenProvider,
                                  TokenStore tokenStore,
                                  UserRolePermissionGateway roleGateway,
                                  DomainEventPublisher eventPublisher) {
        this.credentialRepository = credentialRepository;
        this.passwordHasher = passwordHasher;
        this.tokenProvider = tokenProvider;
        this.tokenStore = tokenStore;
        this.roleGateway = roleGateway;
        this.eventPublisher = eventPublisher;
    }

    // ============================================================
    // 用例 1：注册
    // ============================================================

    /**
     * 注册新账号。
     *
     * <p><b>编排步骤：</b>
     * <ol>
     *   <li>唯一性校验（username / mobile）。</li>
     *   <li>调用 {@link UserCredential#register} 工厂方法创建聚合。</li>
     *   <li>持久化。</li>
     *   <li>发布 {@link AccountCreatedEvent}（触发 user-service 建业务档案）。</li>
     * </ol>
     *
     * @return userId
     */
    @Transactional
    public String register(RegisterCommand cmd) {
        if (credentialRepository.existsByUsername(cmd.username())) {
            throw new BusinessException(ErrorCode.AUTH_USERNAME_EXISTS);
        }
        if (credentialRepository.existsByMobile(cmd.mobile())) {
            throw new BusinessException(ErrorCode.USER_MOBILE_DUPLICATE);
        }

        String userId = IdGenerator.nextIdStr();
        UserCredential credential = UserCredential.register(
                userId, cmd.username(), cmd.rawPassword(),
                cmd.mobile(), cmd.email(), passwordHasher);
        credentialRepository.save(credential);

        // 发布跨上下文事件（通知 user-service 建档）
        AccountCreatedEvent event = new AccountCreatedEvent(
                userId, cmd.username(),
                cmd.nickname() == null || cmd.nickname().isBlank() ? cmd.username() : cmd.nickname(),
                cmd.mobile(), cmd.email());
        safePublish(event);

        log.info("[Auth] registered userId={} username={}", userId, cmd.username());
        return userId;
    }

    // ============================================================
    // 用例 2：登录
    // ============================================================

    /**
     * 登录。
     *
     * <p><b>编排步骤：</b>
     * <ol>
     *   <li>根据 username 加载 Credential 聚合。</li>
     *   <li>{@link UserCredential#assertCanLogin()}：状态可用 + 未锁定。</li>
     *   <li>{@link PasswordHasher#matches}：校验密码。
     *       <ul>
     *         <li>失败：{@link UserCredential#recordLoginFailure()}（可能触发锁定）+ 抛异常。</li>
     *         <li>成功：{@link UserCredential#recordLoginSuccess(String)}。</li>
     *       </ul>
     *   </li>
     *   <li>Feign 拉取角色权限。</li>
     *   <li>签发 AccessToken (JWT) + RefreshToken (opaque)。</li>
     *   <li>RefreshToken 存 Redis。</li>
     *   <li>持久化 Credential（更新失败计数/最近登录时间）。</li>
     * </ol>
     */
    @Transactional
    public TokenPairDTO login(LoginCommand cmd) {
        UserCredential credential = credentialRepository.findByUsername(cmd.username())
                .orElseThrow(() -> {
                    // 安全考虑：不告诉调用者"用户名不存在"，统一返回密码错误
                    return new BusinessException(ErrorCode.AUTH_PASSWORD_INCORRECT,
                            "用户名或密码错误");
                });

        credential.assertCanLogin();

        if (!passwordHasher.matches(cmd.password(), credential.getPasswordHash())) {
            credential.recordLoginFailure();
            credentialRepository.save(credential);
            throw new BusinessException(ErrorCode.AUTH_PASSWORD_INCORRECT, "用户名或密码错误");
        }

        credential.recordLoginSuccess(cmd.ip());
        credentialRepository.save(credential);

        // 拉取角色权限（Feign 调用 user-service）
        Set<String> roles = roleGateway.fetchRoles(credential.getUserId());
        Set<String> permissions = roleGateway.fetchPermissions(credential.getUserId());

        return issueTokenPair(credential, roles, permissions);
    }

    // ============================================================
    // 用例 3：刷新 Token
    // ============================================================

    /**
     * 用 RefreshToken 换取新的双 Token。
     *
     * <p><b>Token Rotation：</b>
     * 每次刷新都会<b>作废旧 RefreshToken</b>，签发新的对。这是 OAuth 2.0 的最佳实践，
     * 可以缓解 RefreshToken 泄露后的持续滥用。</p>
     */
    @Transactional
    public TokenPairDTO refresh(String refreshToken) {
        TokenStore.RefreshTokenInfo info = tokenStore.findRefreshToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));

        UserCredential credential = credentialRepository.findById(info.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));
        credential.assertCanLogin();

        // 作废旧 RefreshToken
        tokenStore.deleteRefreshToken(info.userId(), info.tokenId());

        Set<String> roles = roleGateway.fetchRoles(credential.getUserId());
        Set<String> permissions = roleGateway.fetchPermissions(credential.getUserId());
        return issueTokenPair(credential, roles, permissions);
    }

    // ============================================================
    // 用例 4：注销
    // ============================================================

    /**
     * 注销当前会话。
     *
     * @param accessToken  当前请求的 AccessToken（从中提取 tokenId 与剩余 TTL）
     * @param refreshToken 可选，客户端主动上交则一并作废；否则删除该用户所有 RefreshToken
     */
    public void logout(String accessToken, String refreshToken) {
        TokenProvider.AccessTokenPayload payload = tokenProvider.parseAccessToken(accessToken);
        if (payload != null) {
            long remaining = payload.expiresAt() - System.currentTimeMillis() / 1000;
            if (remaining > 0) {
                tokenStore.blacklistAccessToken(payload.tokenId(), Duration.ofSeconds(remaining));
            }
            tokenStore.deleteAllRefreshTokens(payload.userId());
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            tokenStore.findRefreshToken(refreshToken).ifPresent(info ->
                    tokenStore.deleteRefreshToken(info.userId(), info.tokenId()));
        }
        log.info("[Auth] logout success");
    }

    // ============================================================
    // 用例 5：修改密码
    // ============================================================

    /**
     * 修改密码。
     *
     * <p>修改成功后：<b>强制该用户所有已登录设备下线</b>（删除所有 RefreshToken）。
     * AccessToken 因为是无状态的，会在剩余有效期内继续可用，
     * 生产环境可通过"密码修改时间戳"字段做二次校验（本项目简化处理）。</p>
     */
    @Transactional
    public void changePassword(ChangePasswordCommand cmd) {
        UserCredential credential = credentialRepository.findById(cmd.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));
        credential.changePassword(cmd.oldPassword(), cmd.newPassword(), passwordHasher);
        credentialRepository.save(credential);

        // 强制所有会话失效
        tokenStore.deleteAllRefreshTokens(cmd.userId());

        // 发布事件
        credential.getDomainEvents().forEach(this::safePublish);
        credential.clearDomainEvents();

        log.info("[Auth] password changed userId={}", cmd.userId());
    }

    // ============================================================
    // 用例 6：解析 Token（供其他服务通过 Feign 调用）
    // ============================================================

    /**
     * 解析 AccessToken，返回当前用户信息。
     * <p>其他服务通过 {@code AuthFeignClient#parseToken} 调用本方法。</p>
     */
    public TokenProvider.AccessTokenPayload parseToken(String accessToken) {
        if (accessToken == null) return null;
        String token = accessToken.startsWith("Bearer ") ? accessToken.substring(7) : accessToken;
        TokenProvider.AccessTokenPayload payload = tokenProvider.parseAccessToken(token);
        if (payload == null) return null;
        // 检查黑名单
        if (tokenStore.isAccessTokenBlacklisted(payload.tokenId())) {
            return null;
        }
        return payload;
    }

    /**
     * 获取用户权限（带 Redis 缓存优化留给实现层，此处直连网关）。
     */
    public Set<String> getUserPermissions(String userId) {
        return roleGateway.fetchPermissions(userId);
    }

    // ============================================================
    // 内部工具
    // ============================================================

    private TokenPairDTO issueTokenPair(UserCredential credential, Set<String> roles, Set<String> permissions) {
        String tokenId = UUID.randomUUID().toString().replace("-", "");
        String accessToken = tokenProvider.issueAccessToken(
                credential.getUserId(), credential.getUsername(), tokenId, roles, permissions);
        String refreshTokenId = UUID.randomUUID().toString().replace("-", "");
        String refreshToken = tokenProvider.issueRefreshToken(credential.getUserId(), refreshTokenId);

        tokenStore.saveRefreshToken(credential.getUserId(), refreshTokenId, refreshToken,
                Duration.ofSeconds(tokenProvider.getRefreshTokenTtlSeconds()));

        return TokenPairDTO.of(accessToken, refreshToken,
                tokenProvider.getAccessTokenTtlSeconds(),
                credential.getUserId(), credential.getUsername());
    }

    private void safePublish(DomainEvent event) {
        try {
            eventPublisher.publish(event);
        } catch (Exception e) {
            log.error("[Auth] publish event failed: {}", event, e);
        }
    }
}
