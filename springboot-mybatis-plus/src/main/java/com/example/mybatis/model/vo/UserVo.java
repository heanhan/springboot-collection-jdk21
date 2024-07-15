package com.example.mybatis.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @Description : TODO
 * @Author : zhaojh
 * @Date : 2024/7/12 14:10
 */

@Data
@Tag(name = "UserVo对象", description = "测试实体类")
public class UserVo {

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名")
    private String username;

    @Schema(description = "性别")
    private String sex;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "联系方式")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮件")
    private String email;

    @Schema(description = "班级名称")
    private String className;

    @Schema(description = "逻辑删除 1有效；0删除")
    private Integer deleted;
}
