package com.example.dynamic.jpa.system.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DestroyTenantReq {
    @NotBlank(message = "请输入租户编码确认销毁")
    private String tenantCode;
}
