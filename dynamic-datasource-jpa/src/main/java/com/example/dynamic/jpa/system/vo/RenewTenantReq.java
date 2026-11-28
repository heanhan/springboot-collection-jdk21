package com.example.dynamic.jpa.system.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RenewTenantReq {
    @NotNull(message = "续期天数不能为空")
    @Min(value = 1, message = "续期天数至少为1天")
    @Max(value = 3650, message = "单次续期不能超过10年")
    private Integer days;
}
