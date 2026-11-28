package com.example.dynamic.jpa.system.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RetryProvisionReq {
    @Pattern(regexp = "^[a-zA-Z0-9_@.]{5,16}$", message = "管理员用户名格式错误")
    private String adminUsername;

    @Size(min = 8, max = 64, message = "管理员密码长度为8-64位")
    private String adminPassword;

    @Size(max = 16, message = "管理员手机号长度不能超过16")
    private String adminPhone;

    @Email(message = "管理员邮箱格式错误")
    @Size(max = 64, message = "管理员邮箱长度不能超过64")
    private String adminEmail;
}
