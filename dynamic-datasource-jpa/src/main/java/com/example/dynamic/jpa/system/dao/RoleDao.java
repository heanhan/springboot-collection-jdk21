package com.example.dynamic.jpa.system.dao;

import com.example.dynamic.jpa.system.entity.Role;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * <p>
 * 角色表 Mapper 接口
 * </p>
 *
 * @author zhaojh
 */
@Repository
public interface RoleDao extends BaseRepository<Role,Integer>, JpaSpecificationExecutor<Role> {

}
