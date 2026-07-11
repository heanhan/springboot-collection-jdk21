package com.example.oauth.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * 系统权限实体
 * <p>
 * 权限粒度到接口级别，例如 user:create、user:delete。
 * 与角色的关系不再使用 @ManyToMany，改由关联实体 SysRolePermission 显式映射。
 */
@Data
@Entity
@Table(name = "sys_permission")
public class SysPermission implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "snowflakeId")
    @GenericGenerator(name = "snowflakeId", strategy = "com.example.oauth.config.SnowflakeIdGenerator")
    @Column(name = "id")
    private Long id;

    /** 权限名称 */
    @Column(name = "perm_name", length = 64, nullable = false)
    private String permName;

    /** 权限编码（如 user:create） */
    @Column(name = "perm_code", length = 128, unique = true, nullable = false)
    private String permCode;

    /** 权限类型：1-菜单 2-按钮 3-接口 */
    @Column(name = "type")
    private Integer type;

    /** 关联的 URL（接口权限时填写） */
    @Column(name = "url", length = 256)
    private String url;

    /** HTTP 方法（GET/POST/PUT/DELETE 等，逗号分隔） */
    @Column(name = "method", length = 64)
    private String method;

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

    @PrePersist
    public void prePersist() {
        if (this.createTime == null) {
            this.createTime = new Date();
        }
    }
}
