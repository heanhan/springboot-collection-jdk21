package com.example.parameter.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserDto implements Serializable {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户编号
     */
    private String UserId;

    /**
     *姓名
     */
    private String useName;

    /**
     *性别
     */
    private Boolean sex;

    /**
     *身高
     */
    private Double height;

    /**
     *出生日期
     */
    private LocalDate birthDay;

    /**
     *创建时间
     */
    private LocalDateTime createTime;

    /**
     *最后更新维护时间
     */
    private LocalDateTime updateTime;
}
