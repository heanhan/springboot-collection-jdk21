package org.example.redis.lettuce.pojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 用户实体类
 */
@Data
@Entity(name = "t_sys_user")
@DynamicInsert
@DynamicUpdate
@EqualsAndHashCode(callSuper = true)
public class SysUser extends BaseEntity {
    /**
     * 用户名
     */
    @Column(unique = true)
    private String username;
    /**
     * 密码
     */
    private String password;
    /**
     * 电话
     */
    private String phone;
    /**
     * 个人介绍
     */
    @Lob
    private String introduce;
    /**
     * 所属部门ID
     */
    private Long departmentId;

}

