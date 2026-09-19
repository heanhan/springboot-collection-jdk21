package com.example.ddd.user.infrastructure.persistence.dao;

import com.example.ddd.user.infrastructure.persistence.po.UserRolePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * DAO：用户-角色关联。
 */
public interface UserRoleDao extends JpaRepository<UserRolePO, Long> {

    @Query("SELECT ur FROM UserRolePO ur WHERE ur.userId = :userId")
    List<UserRolePO> findByUserId(@Param("userId") String userId);

    @Modifying
    @Query("DELETE FROM UserRolePO ur WHERE ur.userId = :userId")
    int deleteByUserId(@Param("userId") String userId);
}
