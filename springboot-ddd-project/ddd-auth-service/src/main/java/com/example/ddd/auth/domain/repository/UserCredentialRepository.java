package com.example.ddd.auth.domain.repository;

import com.example.ddd.auth.domain.model.aggregate.UserCredential;

import java.util.Optional;

/**
 * 仓储接口：UserCredential 聚合根。
 *
 * <p>放在 domain 层，实现在 infrastructure 层（JPA + MySQL）。</p>
 */
public interface UserCredentialRepository {

    Optional<UserCredential> findById(String userId);

    Optional<UserCredential> findByUsername(String username);

    Optional<UserCredential> findByMobile(String mobile);

    boolean existsByUsername(String username);

    boolean existsByMobile(String mobile);

    void save(UserCredential credential);
}
