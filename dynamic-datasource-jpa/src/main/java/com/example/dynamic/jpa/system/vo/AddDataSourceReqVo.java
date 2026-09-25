package com.example.dynamic.jpa.system.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;


/**
 * @Author: zhaojh
 * @ClassName: AddDataSourceReqVo
 * @Description: 添加租户数据源信息请求VO
 */
@Data
public class AddDataSourceReqVo {

    @NotNull(message = "租户id不能为空")
    @Positive(message = "租户id必须大于0")
    private Integer tenantId;

    @NotBlank(message = "数据库url不能为空")
    private String url;

    @NotBlank(message = "数据库username不能为空")
    private String username;

    @NotBlank(message = "数据库password不能为空")
    @lombok.ToString.Exclude
    private String password;
}
