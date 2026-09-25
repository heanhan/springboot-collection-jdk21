package com.example.dynamic.jpa.system.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;

/**
 * 刷新 token 请求。
 *
 * @author zhaojh
 */
@Data
public class RefreshReqVo {

    @NotBlank(message = "refreshToken不能为空")
    @ToString.Exclude
    private String refreshToken;
}
