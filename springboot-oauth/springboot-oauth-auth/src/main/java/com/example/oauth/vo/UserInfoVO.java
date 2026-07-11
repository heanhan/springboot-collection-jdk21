package com.example.oauth.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 当前登录用户信息 VO
 */
@Data
public class UserInfoVO {

    private Long userId;

    private String username;

    private String nickname;

    private String avatar;

    private String email;

    private String phone;

    /** 角色编码列表 */
    private Set<String> roles;

    /** 权限编码列表 */
    private Set<String> permissions;

    /** 登录时间 */
    private Date loginTime;
}
