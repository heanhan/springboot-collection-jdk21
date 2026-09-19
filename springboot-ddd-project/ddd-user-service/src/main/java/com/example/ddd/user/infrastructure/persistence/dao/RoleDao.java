package com.example.ddd.user.infrastructure.persistence.dao;

import com.example.ddd.user.infrastructure.persistence.po.RolePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * DAO：角色。
 */
public interface RoleDao extends JpaRepository<RolePO, String> {

    @Query("SELECT r FROM RolePO r WHERE r.roleCode = :code AND r.deleted = 0")
    Optional<RolePO> findByCode(@Param("code") String code);

    @Query("SELECT r FROM RolePO r WHERE r.roleId IN :ids AND r.deleted = 0")
    List<RolePO> findByIdIn(@Param("ids") Collection<String> ids);

    @Query("SELECT r FROM RolePO r WHERE r.deleted = 0 ORDER BY r.roleCode ASC")
    List<RolePO> findAllNotDeleted();
}
