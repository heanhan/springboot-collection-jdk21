package com.example.mybatis.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author zhaojh
 * @since 2024-07-12
 */
@Data
@TableName("user")
public class User implements Serializable {

    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 性别
     */
    private String sex;

    /**
     *密码
     */
    private String password;

    /**
     *联系方式
     */
    private String phone;

    /**
     *邮箱
     */
    private String email;

    /**
     *班级名称
     */
    private String className;

    /**
     *
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     *
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     *逻辑删除 1有效；0删除
     */
    private Integer deleted;
}
