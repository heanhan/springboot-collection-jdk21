package com.example.mybatis.controller;

import com.example.common.enums.CommonEnum;
import com.example.common.result.ResultBody;
import com.example.mybatis.entity.User;
import com.example.mybatis.model.dto.UserDto;
import com.example.mybatis.model.vo.UserVo;
import com.example.mybatis.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author zhaojh
 * @since 2024-07-12
 */

@Tag(name = "用户管理")
@Validated
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Operation(summary = "添加用户")
    @PostMapping("/addUser")
    public ResultBody<String> addUser(@RequestBody @Validated UserVo userVo) {
        Boolean flag = userService.addUser(userVo);
        if (flag) {
            return ResultBody.success();
        }
        return ResultBody.error("添加失败!");
    }

    @Operation(summary = "根据id 查询用户")
    @GetMapping("/findUserById")
    public ResultBody<UserDto> findUserById(Long id) {
        UserDto dto = userService.findUserById(id);
        return ResultBody.success(dto);
    }

}
