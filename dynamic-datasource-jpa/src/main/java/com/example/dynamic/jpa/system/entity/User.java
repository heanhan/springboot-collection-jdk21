package com.example.dynamic.jpa.system.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 系統用戶表
 * </p>
 *
 * @author zhaojh
 * @since 2022-11-04
 */
@Data
@Entity
@Table(name ="sys_user")
@DynamicInsert
@DynamicUpdate
public class User implements Serializable {

    private static final long serialVersionUID = 6648167273825216429L;

    /**
     * 用户id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @NotNull(message = "roleId不能为空", groups = {Delete.class, Edit.class, Get.class})
    private Integer id;

    @Column(name = "tenant_id")
    private Integer tenantId;

    /**
     * 用户名
     */
    @Column(name = "username")
    @NotBlank(message = "username不能为空", groups = {Add.class})
    @Pattern(regexp = "^[a-zA-Z0-9_@.]{5,16}$", message = "格式错误", groups = {Add.class})
    private String username;

    /**
     * 昵称
     */
    @Column(name = "nickname")
    private String nickname;

    /**
     * 密码
     */
    @Column(name = "password")
    private String password;

    /**
     * 邮箱
     */
    @Column(name = "email")
    private String email;

    /**
     * 电话号码
     */
    @Column(name = "phone")
    @NotBlank(message = "phone不能为空", groups = {Add.class})
    private String phone;

    /**
     * 头像
     */
    @Column(name = "avatar")
    private String avatar;

    /**
     * 角色id
     */
    @Column(name = "role_id")
    @NotNull(message = "roleId不能为空", groups = {Add.class})
    private Integer roleId;

    /**
     * 备注
     */
    @Column(name = "remark")
    private String remark;

    /**
     * 用户类型
     */
    @Column(name = "type")
    private Integer type;

    /**
     * 状态
     */
    @Column(name = "status")
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH-mm-ss")
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH-mm-ss")
    @Column(name = "create_time")
    private LocalDateTime createTime;


    public interface Add {

    }

    public interface Edit {

    }

    public interface Delete {

    }

    public interface Get {

    }


}
