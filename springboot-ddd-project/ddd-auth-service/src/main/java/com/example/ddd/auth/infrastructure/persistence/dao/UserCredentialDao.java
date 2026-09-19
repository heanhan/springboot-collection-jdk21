package com.example.ddd.auth.infrastructure.persistence.dao;

import com.example.ddd.auth.infrastructure.persistence.po.UserCredentialPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * DAO：认证凭据。
 */
public interface UserCredentialDao extends JpaRepository<UserCredentialPO, String> {

    @Query("SELECT c FROM UserCredentialPO c WHERE c.username = :username AND c.deleted = 0")
    Optional<UserCredentialPO> findByUsername(@Param("username") String username);

    @Query("SELECT c FROM UserCredentialPO c WHERE c.mobile = :mobile AND c.deleted = 0")
    Optional<UserCredentialPO> findByMobile(@Param("mobile") String mobile);

    @Query("SELECT c FROM UserCredentialPO c WHERE c.userId = :userId AND c.deleted = 0")
    Optional<UserCredentialPO> findByIdAndNotDeleted(@Param("userId") String userId);

    @Query("SELECT COUNT(c) > 0 FROM UserCredentialPO c WHERE c.username = :username AND c.deleted = 0")
    boolean existsByUsernameNotDeleted(@Param("username") String username);

    @Query("SELECT COUNT(c) > 0 FROM UserCredentialPO c WHERE c.mobile = :mobile AND c.deleted = 0")
    boolean existsByMobileNotDeleted(@Param("mobile") String mobile);
}
