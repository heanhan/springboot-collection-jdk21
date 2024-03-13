package com.example.validation.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class UserDto {

    @NotBlank(message = "用户姓名不能为空")
    private String name;

    @NotBlank(message = "密码不能为空")
    private String password;

    @Min(value = 0, message = "年龄不能小于0岁")
    @Max(value = 150, message = "年龄不应超过150岁")
    private Integer age;

    @NotBlank(message = "手机号码不能为空")
    @Pattern(regexp = "/^[1][3,4,5,6,7,8,9][0-9]{9}$/", message = "手机号格式不正确")
    private String phone;

    @NotNull(message = "Email不能为空")
    @Email(message = "Email格式不正确")
    private String email;

    @Pattern(regexp = "^[\\u4e00-\\u9fa5]{0,}$",message = "属性只能为中文")
    private String chinaWord;




}
