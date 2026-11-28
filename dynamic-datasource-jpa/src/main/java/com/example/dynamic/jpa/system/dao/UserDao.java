package com.example.dynamic.jpa.system.dao;

import com.example.dynamic.jpa.system.entity.User;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDao extends BaseRepository<User, Integer>, JpaSpecificationExecutor<User> {

    Optional<User> findFirstByUsernameAndTenantIdAndStatus(String username, Integer tenantId, Integer status);

    Optional<User> findByIdAndStatus(Integer id, Integer status);

    List<User> findAllByTenantId(Integer tenantId);

    List<User> findAllByTenantIdAndStatusOrderByIdAsc(Integer tenantId, Integer status);

    boolean existsByUsernameAndTenantId(String username, Integer tenantId);

    boolean existsByPhoneAndTenantIdAndStatus(String phone, Integer tenantId, Integer status);

    boolean existsByPhoneAndTenantIdAndIdNotAndStatus(String phone, Integer tenantId, Integer id, Integer status);

    boolean existsByRoleIdAndStatus(Integer roleId, Integer status);
}
