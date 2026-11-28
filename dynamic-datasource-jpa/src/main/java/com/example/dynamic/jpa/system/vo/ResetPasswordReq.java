package com.example.dynamic.jpa.system.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordReq {
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度为8-64位")
    private String newPassword;
}
