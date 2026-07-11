package com.example.oauth.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * 系统角色实体
 * <p>
 * 角色与权限/菜单/用户的关系不再使用 @ManyToMany/@JoinTable，
 * 改由关联实体 SysRolePermission / SysRoleMenu / SysUserRole 显式映射。
 */
@Data
@Entity
@Table(name = "sys_role")
public class SysRole implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "snowflakeId")
    @GenericGenerator(name = "snowflakeId", strategy = "com.example.oauth.config.SnowflakeIdGenerator")
    @Column(name = "id")
    private Long id;

    /** 角色名称 */
    @Column(name = "role_name", length = 64, nullable = false)
    private String roleName;

    /** 角色编码（如 ROLE_ADMIN） */
    @Column(name = "role_code", length = 64, unique = true, nullable = false)
    private String roleCode;

    /** 描述 */
    @Column(name = "description", length = 256)
    private String description;

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
