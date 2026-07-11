package com.example.oauth.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * 系统用户实体
 * <p>
 * 用户与角色的关系不再使用 @ManyToMany/@JoinTable，改由关联实体 SysUserRole 显式映射，
 * 角色/权限信息通过 Repository 连表查询获取。
 */
@Data
@Entity
@Table(name = "sys_user")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "snowflakeId")
    @GenericGenerator(name = "snowflakeId", strategy = "com.example.oauth.config.SnowflakeIdGenerator")
    @Column(name = "id")
    private Long id;

    /** 用户名 */
    @Column(name = "username", length = 64, unique = true, nullable = false)
    private String username;

    /** 密码（BCrypt 加密） */
    @Column(name = "password", length = 128, nullable = false)
    private String password;

    /** 昵称 */
    @Column(name = "nickname", length = 64)
    private String nickname;

    /** 手机号 */
    @Column(name = "phone", length = 20)
    private String phone;

    /** 邮箱 */
    @Column(name = "email", length = 128)
    private String email;

    /** 头像 URL */
    @Column(name = "avatar", length = 256)
    private String avatar;

    /** 性别：0-未知 1-男 2-女 */
    @Column(name = "gender")
    private Integer gender;

    /** 状态：0-禁用 1-启用 */
    @Column(name = "status")
    private Integer status;

    /** 创建时间 */
    @Column(name = "create_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;

    /** 更新时间 */
    @Column(name = "update_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateTime;

    @PrePersist
    public void prePersist() {
        if (this.createTime == null) {
            this.createTime = new Date();
        }
        this.updateTime = new Date();
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = new Date();
    }
}
