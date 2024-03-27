package com.examle.security6.dao;

import com.examle.security6.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface RoleMapper extends JpaRepository<RoleEntity,Long>, JpaSpecificationExecutor<RoleEntity> {

    @Query(value = "select tr.id, tr.role  from sys_user_role tur " +
            " left join sys_role tr on tur.role = tr.role " +
            " where tur.username =?1  "
            ,nativeQuery = true)
    List<RoleEntity> selectRoleByUsername(String username);

}
