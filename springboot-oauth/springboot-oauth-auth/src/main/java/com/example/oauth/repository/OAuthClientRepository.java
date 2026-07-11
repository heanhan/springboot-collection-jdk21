package com.example.oauth.repository;

import com.example.oauth.entity.OAuthClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * OAuth 客户端 Repository（JDBC 存储）
 */
@Repository
public interface OAuthClientRepository extends JpaRepository<OAuthClient, Long> {

    /** 按 client_id 查询 */
    OAuthClient findByClientId(String clientId);

    /** client_id 是否存在 */
    boolean existsByClientId(String clientId);

    /** 按客户端名称模糊分页查询 */
    Page<OAuthClient> findByClientNameContaining(String clientName, Pageable pageable);
}
