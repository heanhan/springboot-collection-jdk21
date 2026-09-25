package com.example.dynamic.jpa.security;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author zhaojh
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class LoginUser implements Serializable {
    private static final long serialVersionUID = 1158761213706993647L;
    @jakarta.validation.constraints.NotBlank(message = "用户名不能为空")
    private String username;
    @jakarta.validation.constraints.NotBlank(message = "密码不能为空")
    @lombok.ToString.Exclude
    private String password;
    private Integer tenantid;
}