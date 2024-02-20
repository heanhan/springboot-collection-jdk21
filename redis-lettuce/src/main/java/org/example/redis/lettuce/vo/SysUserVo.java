package org.example.redis.lettuce.vo;

import lombok.Data;

@Data
public class SysUserVo {

    /**
     * 用户名
     */
    private String username;

    /**
     * 电话
     */
    private String phone;

    /**
     * 所属部门ID
     */
    private Long departmentId;
}
