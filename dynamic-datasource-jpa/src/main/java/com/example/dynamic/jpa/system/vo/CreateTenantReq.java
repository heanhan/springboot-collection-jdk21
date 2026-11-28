package com.example.dynamic.jpa.system.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateTenantReq {

    @NotBlank(message = "租户名称不能为空")
    @Size(max = 64, message = "租户名称长度不能超过64")
    private String tenantName;

    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{2,31}$", message = "租户编码格式错误")
    private String tenantCode;

    @NotBlank(message = "联系电话不能为空")
    @Size(max = 16, message = "联系电话长度不能超过16")
    private String phone;

    @Email(message = "邮箱格式错误")
    @Size(max = 64, message = "邮箱长度不能超过64")
    private String email;

    private LocalDateTime expiresAt;

    @NotBlank(message = "管理员用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_@.]{5,16}$", message = "管理员用户名格式错误")
    private String adminUsername;

    @NotBlank(message = "管理员密码不能为空")
    @Size(min = 8, max = 64, message = "管理员密码长度为8-64位")
    private String adminPassword;

    @NotBlank(message = "管理员手机号不能为空")
    @Size(max = 16, message = "管理员手机号长度不能超过16")
    private String adminPhone;

    @Email(message = "管理员邮箱格式错误")
    @Size(max = 64, message = "管理员邮箱长度不能超过64")
    private String adminEmail;
}
