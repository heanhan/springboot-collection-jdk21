package com.example.ddd.user.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 请求 DTO：新增 / 修改收货地址。
 */
public record SaveAddressRequest(
        @NotBlank @Size(max = 64) String receiver,
        @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String mobile,
        @NotBlank @Size(max = 32) String province,
        @NotBlank @Size(max = 32) String city,
        @NotBlank @Size(max = 32) String district,
        @NotBlank @Size(max = 255) String detail,
        @Size(max = 10) String zipCode,
        String tag,
        boolean setAsDefault
) {
}
