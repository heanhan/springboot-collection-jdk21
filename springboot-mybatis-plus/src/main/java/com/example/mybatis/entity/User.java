package com.example.mybatis.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * @Description : TODO
 * @Author : zhaojh
 * @Date : 2024/7/6 09:31
 */

@Data
public class User implements Serializable {

    //id 主键
    private Long id;

    //用户名
    private String username;

    //性别
    private String sex;

    //密码
    private String password;

    //联系方式
    private String phone;

    //邮箱
    private String email;

    //班级名称
    private String className;

    //逻辑删除： 1 有效；0 无效
    private Integer deleted;
}
