package com.example.oauth.dto;

import lombok.Data;

/**
 * 用户自助修改资料请求 DTO
 * <p>
 * 仅允许修改昵称、手机号、邮箱、头像、性别，不涉及用户名/密码/状态/角色。
 */
@Data
public class ProfileUpdateDTO {

    /** 昵称 */
    private String nickname;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 头像 URL */
    private String avatar;

    /** 性别：0-未知 1-男 2-女 */
    private Integer gender;
}
