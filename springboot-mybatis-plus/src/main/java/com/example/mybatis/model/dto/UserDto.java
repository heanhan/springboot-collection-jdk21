package com.example.mybatis.model.dto;

import lombok.Data;

/**
 * @Description : TODO
 * @Author : zhaojh
 * @Date : 2024/7/12 14:09
 */

@Data
public class UserDto {

    //id 主键
    private Long id;

    //用户名
    private String username;

    //性别
    private String sex;

    //联系方式
    private String phone;

    //邮箱
    private String email;

    //班级名称
    private String className;

    //逻辑删除： 1 有效；0 无效
    private Integer deleted;
}
