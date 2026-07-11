package com.example.oauth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 用户新增/更新请求 DTO
 * <p>
 * 新增时 password 必填；更新时 password 为空表示不修改密码。
 */
@Data
public class UserSaveDTO {

    /** 用户 ID（更新时必填，新增时留空） */
    private Long id;

    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 密码（新增必填；更新为空则不修改） */
    private String password;

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

    /** 状态：0-禁用 1-启用 */
    private Integer status;
}
