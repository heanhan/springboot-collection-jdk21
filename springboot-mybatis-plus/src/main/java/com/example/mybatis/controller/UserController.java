package com.example.mybatis.controller;

import com.example.common.enums.CommonEnum;
import com.example.common.result.ResultBody;
import com.example.mybatis.entity.User;
import com.example.mybatis.model.dto.UserDto;
import com.example.mybatis.model.vo.SelectUserVo;
import com.example.mybatis.model.vo.UpdateUserVo;
import com.example.mybatis.model.vo.UserVo;
import com.example.mybatis.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    public ResultBody<UserDto> findUserById(@NotNull(message = "参数 Id 不允许为空") Long id) {
        UserDto dto = userService.findUserById(id);
        return ResultBody.success(dto);
    }

    @PostMapping(value = "/findAllUserList")
    public ResultBody<Object> findAllUserList(@RequestBody SelectUserVo vo){
        List<User> userList = userService.findAllUserList(vo);
        return ResultBody.success(userList);
    }

    /**
     * 更新用户信息
     * @param vo
     * @return
     */
    @PostMapping(value = "/updateUserInfo")
    public ResultBody<Object> updateUserInfo(@RequestBody @Validated UpdateUserVo vo){
        Boolean flag =  userService.updateUserInfo(vo);
        if (flag){
            return ResultBody.success();
        }
        return ResultBody.error(CommonEnum.ERROR_UPDATE);
    }

    @GetMapping(value = "/deleteUserById")
    public ResultBody<Void> deleteUserById(@NotNull(message = "删除参数id 不能为空") Long id) {
        Boolean flag = userService.deleteUserById(id);
        if (flag){
            return ResultBody.success();
        }
        return ResultBody.error(CommonEnum.ERROR_DELETED);
    }



}
