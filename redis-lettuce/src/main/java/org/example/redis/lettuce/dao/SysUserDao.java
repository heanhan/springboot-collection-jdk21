package org.example.redis.lettuce.dao;

import org.example.redis.lettuce.pojo.SysUser;
import org.example.redis.lettuce.vo.SysUserVo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SysUserDao extends JpaRepository<SysUser,Long>, JpaSpecificationExecutor<SysUser> {


    /**
     * 根据条件查询用户
     *
     * @param sysUserVo 查询条件
     * @return list
     */
    @Query(value = "select * from t_sys_user where username =:#{#sysUserVo.username}",nativeQuery = true)
    List<SysUser> findSysUserByCondition(@Param("sysUserVo") SysUserVo sysUserVo);
}
