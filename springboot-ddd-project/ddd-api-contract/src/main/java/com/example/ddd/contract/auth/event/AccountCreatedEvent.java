package com.example.ddd.contract.auth.event;

import com.example.ddd.common.domain.event.AbstractDomainEvent;

/**
 * 跨上下文领域事件：认证账号已创建 (AccountCreated)。
 *
 * <p><b>发布者：</b>auth-service（注册接口成功时）。</p>
 * <p><b>订阅者：</b>user-service（创建用户业务档案）。</p>
 *
 * <p><b>为什么放在 ddd-api-contract？</b>
 * 跨上下文的事件是<b>发布语言 (Published Language)</b> 的一部分，
 * 生产者和消费者必须共享同一个 Schema，否则反序列化会失败。
 * 契约模块就是这类"跨上下文共识"的存放地。</p>
 *
 * @author ddd-learning
 */
public class AccountCreatedEvent extends AbstractDomainEvent {

    private static final long serialVersionUID = 1L;

    /** 认证账号 ID（与 user-service 中的 userId 一致） */
    private String userId;
    /** 登录用户名 */
    private String username;
    /** 昵称 */
    private String nickname;
    /** 手机号 */
    private String mobile;
    /** 邮箱 */
    private String email;

    /** Jackson 反序列化用 */
    public AccountCreatedEvent() {
        super("deserialization-placeholder");
    }

    public AccountCreatedEvent(String userId, String username, String nickname, String mobile, String email) {
        super(userId);
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.mobile = mobile;
        this.email = email;
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getNickname() { return nickname; }
    public String getMobile() { return mobile; }
    public String getEmail() { return email; }

    public void setUserId(String userId) { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public void setEmail(String email) { this.email = email; }
}
