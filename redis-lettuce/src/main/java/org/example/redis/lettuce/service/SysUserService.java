package org.example.redis.lettuce.service;

import org.example.redis.lettuce.pojo.SysUser;
import org.example.redis.lettuce.vo.SysUserVo;

import java.util.List;

public interface SysUserService {

    /**
     * 根据条件查询用户
     * @param sysUserVo 查询条件
     * @return list
     */
    List<SysUser> findSysUserByCondition(SysUserVo sysUserVo);

    /**
     * 添加用户
     * @param sysUser
     * @return
     */
    SysUser addSysUser(SysUser sysUser);
}
