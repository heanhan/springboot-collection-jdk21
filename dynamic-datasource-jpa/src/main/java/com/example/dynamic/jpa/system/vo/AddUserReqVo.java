package com.example.dynamic.jpa.system.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增租户账号请求
 *
 * @author zhaojh
 */
@Data
public class AddUserReqVo {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_@.]{5,16}$", message = "用户名格式错误")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度为6-64位")
    @lombok.ToString.Exclude
    private String password;

    @NotNull(message = "角色id不能为空")
    private Integer roleId;

    @NotNull(message = "租户id不能为空")
    @PositiveOrZero(message = "租户id不能为负数")
    private Integer tenantId;

    @Size(max = 32, message = "昵称长度不能超过32")
    private String nickname;

    @Size(max = 16, message = "手机号长度不能超过16")
    private String phone;

    @Email(message = "邮箱格式错误")
    @Size(max = 32, message = "邮箱长度不能超过32")
    private String email;

    private Integer type;
}
