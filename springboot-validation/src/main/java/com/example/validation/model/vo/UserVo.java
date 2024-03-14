package com.example.validation.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Length;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class UserVo {

    @NotBlank(message = "用户姓名不能为空")
    private String name;

    @NotBlank(message = "密码不能为空")
    private String password;

    @Min(value = 0, message = "年龄不能小于0岁")
    @Max(value = 150, message = "年龄不应超过150岁")
    private Integer age;

    private String phone;

    @NotNull(message = "Email不能为空")
    @Email(message = "Email格式不正确")
    private String email;

    @AssertFalse(message = "性别出行传参必须是 false")
    private Boolean sex;

    /**
     * CharSequence子类型、Collection、Map、数组
     */
    @NotEmpty(message = "集合不能为空")
    private List<String> hobby;

    @Pattern(regexp = "^[\\u4e00-\\u9fa5]{0,}$",message = "属性只能为中文")
    private String chinaWord;


    @Past(message = "输入的时间要比当前时间要早")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @Future(message = "输入的时间要比当前时间要晚")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;




}
