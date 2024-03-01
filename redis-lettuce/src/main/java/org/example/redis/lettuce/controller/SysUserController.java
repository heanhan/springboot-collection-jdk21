package org.example.redis.lettuce.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.enums.CommonEnum;
import org.example.commons.result.ResultBody;
import org.example.redis.lettuce.pojo.SysUser;
import org.example.redis.lettuce.service.SysUserService;
import org.example.redis.lettuce.vo.SysUserVo;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequestMapping(value = "/user")
@RestController
public class SysUserController {

    @Resource
    private SysUserService sysUserService;

    @PostMapping(value = "/addSysUser")
    public ResultBody addSysUser(@RequestBody SysUser sysUser){
        SysUser result= sysUserService.addSysUser(sysUser);
        if(ObjectUtils.isEmpty(result)){
            return ResultBody.error(CommonEnum.INTERNAL_SERVER_ERROR);
        }
        return ResultBody.success(CommonEnum.SUCCESS);

    }

    /**
     * 查询分类  动态掉件查询
     * @return
     */
    @PostMapping(value = "/findSysUserByCondition")
    public ResultBody findSysUserByCondition(@RequestBody SysUserVo sysUserVo){
        List<SysUser> list = sysUserService.findSysUserByCondition(sysUserVo);
        return ResultBody.success(list);
    }



}