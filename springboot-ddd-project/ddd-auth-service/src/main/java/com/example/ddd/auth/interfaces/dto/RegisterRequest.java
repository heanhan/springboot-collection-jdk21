package com.example.ddd.auth.interfaces.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 请求 DTO：注册。
 *
 * <p><b>为什么用 record？</b>DTO 是纯数据载体，不可变，record 语义最贴切。</p>
 *
 * <p><b>为什么校验放在 DTO 层而不是领域？</b>
 * 请求格式校验（长度/正则）是"技术校验"，属于协议层职责；
 * 业务规则（用户名唯一、密码不能与旧密码相同）才是领域职责。</p>
 */
public record RegisterRequest(

        @NotBlank(message = "用户名不能为空")
        @Size(min = 4, max = 32, message = "用户名长度必须在 4-32 之间")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 32, message = "密码长度必须在 8-32 之间")
        String password,

        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String mobile,

        @Email(message = "邮箱格式不正确")
        String email,

        @Size(max = 32, message = "昵称长度不能超过 32")
        String nickname
) {
}
