package com.example.dynamic.jpa.system.dao;

import com.example.dynamic.jpa.system.entity.Role;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * <p>
 * 角色表 Mapper 接口
 * </p>
 *
 * @author zhaojh
 */
@Repository
public interface RoleDao extends BaseRepository<Role,Integer>, JpaSpecificationExecutor<Role> {

    Optional<Role> findByIdAndIsDelFalse(Integer id);

    Optional<Role> findByRoleNameAndIsDelFalse(String roleName);

    List<Role> findAllByIsDelFalseOrderByIdAsc();

    boolean existsByRoleNameAndIsDelFalse(String roleName);

    boolean existsByRoleNameAndIdNotAndIsDelFalse(String roleName, Integer id);
}
