package com.example.ddd.user.domain.model.event;

import com.example.ddd.common.domain.event.AbstractDomainEvent;

/**
 * 领域事件：用户已注册 (UserRegistered)。
 *
 * <p><b>发布者：</b>user-service（在 User.register() 工厂方法执行后由应用服务发布）。</p>
 * <p><b>订阅者：</b>营销服务（发新人券）、通知服务（发欢迎邮件）—— 本项目未实现，作扩展点。</p>
 *
 * <p><b>与 auth-service 的 UserRegisteredEvent 关系：</b>
 * auth-service 也会发一个类似事件，用于触发 user-service 创建用户资料。
 * 两者虽然名字类似，但语义不同：
 * <ul>
 *   <li>auth 侧：AccountCreated（凭据创建）</li>
 *   <li>user 侧：UserRegistered（业务用户档案已建立）</li>
 * </ul>
 * 这是限界上下文自治的体现，两个上下文对"注册"这个概念有不同的模型。</p>
 *
 * @author ddd-learning
 */
public class UserRegisteredEvent extends AbstractDomainEvent {

    private static final long serialVersionUID = 1L;

    private String nickname;
    private String mobile;
    private String email;

    public UserRegisteredEvent() {
        super();
    }

    public UserRegisteredEvent(String userId, String nickname, String mobile, String email) {
        super(userId);
        this.nickname = nickname;
        this.mobile = mobile;
        this.email = email;
    }

    public String getNickname() { return nickname; }
    public String getMobile() { return mobile; }
    public String getEmail() { return email; }

    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public void setEmail(String email) { this.email = email; }
}
