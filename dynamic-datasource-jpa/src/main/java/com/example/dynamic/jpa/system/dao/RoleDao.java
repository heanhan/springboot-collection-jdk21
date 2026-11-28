package com.example.dynamic.jpa.system.dao;

import com.example.dynamic.jpa.system.entity.Role;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleDao extends BaseRepository<Role, Integer>, JpaSpecificationExecutor<Role> {

    Optional<Role> findByIdAndIsDelFalse(Integer id);

    Optional<Role> findByRoleCodeAndTenantIdAndIsDelFalse(String roleCode, Integer tenantId);

    List<Role> findAllByTenantIdAndIsDelFalseOrderByIdAsc(Integer tenantId);

    List<Role> findAllByIsDelFalseOrderByIdAsc();

    boolean existsByRoleCodeAndTenantIdAndIsDelFalse(String roleCode, Integer tenantId);

    boolean existsByRoleNameAndTenantIdAndIdNotAndIsDelFalse(String roleName, Integer tenantId, Integer id);
}
