package com.example.mybatis.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description : TODO
 * @Author : zhaojh
 * @Date : 2024/7/30 09:05
 */

@Data
public class UpdateUserVo {

    @NotNull(message = "id 不能为空！")
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
    private LocalDateTime updateTime;

    /**
     *逻辑删除 1有效；0删除
     */
    private Integer deleted;
}
