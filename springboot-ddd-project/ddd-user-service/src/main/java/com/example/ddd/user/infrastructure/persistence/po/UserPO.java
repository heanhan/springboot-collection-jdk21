package com.example.ddd.user.infrastructure.persistence.po;

import com.example.ddd.common.infrastructure.persistence.AbstractJpaAuditablePO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 持久化对象 (PO)：用户表。
 *
 * <p><b>为什么 PO 与领域模型 {@link com.example.ddd.user.domain.model.aggregate.User} 分离？</b>
 * <ul>
 *   <li>PO 面向数据库：字段扁平化、有 JPA 注解、包含审计字段。</li>
 *   <li>领域模型面向业务：包含值对象（Mobile、Email、MemberLevel）、行为方法、事件收集。</li>
 * </ul>
 * 如果混在一起，业务代码会被 JPA 注解污染，且难以做单元测试。</p>
 */
@Entity
@Table(name = "t_user")
public class UserPO extends AbstractJpaAuditablePO {

    @Id
    @Column(name = "user_id", length = 32, nullable = false)
    private String userId;

    @Column(name = "nickname", length = 64, nullable = false)
    private String nickname;

    @Column(name = "avatar", length = 255)
    private String avatar;

    @Column(name = "mobile", length = 20, nullable = false)
    private String mobile;

    @Column(name = "email", length = 128)
    private String email;

    @Column(name = "gender", nullable = false)
    private Integer gender;

    @Column(name = "member_level", length = 16, nullable = false)
    private String memberLevel;

    @Column(name = "status", length = 16, nullable = false)
    private String status;

    @Column(name = "register_time", nullable = false)
    private LocalDateTime registerTime;

    // ============================================================
    // Getters / Setters (JPA 要求)
    // ============================================================

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }
    public String getMemberLevel() { return memberLevel; }
    public void setMemberLevel(String memberLevel) { this.memberLevel = memberLevel; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getRegisterTime() { return registerTime; }
    public void setRegisterTime(LocalDateTime registerTime) { this.registerTime = registerTime; }
}
