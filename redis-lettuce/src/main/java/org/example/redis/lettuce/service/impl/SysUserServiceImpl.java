package org.example.redis.lettuce.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.redis.lettuce.dao.SysUserDao;
import org.example.redis.lettuce.pojo.SysUser;
import org.example.redis.lettuce.service.SysUserService;
import org.example.redis.lettuce.vo.SysUserVo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class SysUserServiceImpl implements SysUserService {

    @Resource
    private SysUserDao sysUserDao;

    /**
     * 根据条件查询用户
     *
     * @param sysUserVo 查询条件
     * @return list
     */
    @Override
    public List<SysUser> findSysUserByCondition(SysUserVo sysUserVo) {
        try{
            return sysUserDao.findSysUserByCondition(sysUserVo);
        }catch (Exception e){
            log.info("异常信息：{}",e.getMessage());
        }
        return null;
    }

    /**
     * 添加用户
     *
     * @param sysUser 用户信息
     * @return 返回创建成功的用户信息
     */
    @Override
    public SysUser addSysUser(SysUser sysUser) {
        try {
            SysUser save = sysUserDao.save(sysUser);
            return save;
        } catch (Exception e) {
            log.info("异常信息：{}",e.getMessage());
        }
        return null;
    }
}
