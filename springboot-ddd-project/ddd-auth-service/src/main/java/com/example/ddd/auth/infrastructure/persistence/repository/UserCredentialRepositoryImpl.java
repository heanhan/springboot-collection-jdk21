package com.example.ddd.auth.infrastructure.persistence.repository;

import com.example.ddd.auth.domain.model.aggregate.UserCredential;
import com.example.ddd.auth.domain.model.valueobject.CredentialStatus;
import com.example.ddd.auth.domain.repository.UserCredentialRepository;
import com.example.ddd.auth.infrastructure.persistence.dao.UserCredentialDao;
import com.example.ddd.auth.infrastructure.persistence.po.UserCredentialPO;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 仓储实现：UserCredential。
 */
@Repository
public class UserCredentialRepositoryImpl implements UserCredentialRepository {

    private final UserCredentialDao dao;

    public UserCredentialRepositoryImpl(UserCredentialDao dao) {
        this.dao = dao;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserCredential> findById(String userId) {
        return dao.findByIdAndNotDeleted(userId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserCredential> findByUsername(String username) {
        return dao.findByUsername(username).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserCredential> findByMobile(String mobile) {
        return dao.findByMobile(mobile).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return dao.existsByUsernameNotDeleted(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByMobile(String mobile) {
        return dao.existsByMobileNotDeleted(mobile);
    }

    @Override
    @Transactional
    public void save(UserCredential credential) {
        UserCredentialPO po = toPO(credential);
        dao.findByIdAndNotDeleted(credential.getUserId()).ifPresent(existing -> {
            po.setCreateTime(existing.getCreateTime());
            po.setVersion(existing.getVersion());
        });
        dao.save(po);
    }

    // ============================================================
    // 转换
    // ============================================================

    private UserCredential toDomain(UserCredentialPO po) {
        return UserCredential.reconstitute(
                po.getUserId(), po.getUsername(), po.getPasswordHash(),
                po.getMobile(), po.getEmail(),
                CredentialStatus.valueOf(po.getStatus()),
                po.getFailCount() == null ? 0 : po.getFailCount(),
                po.getLockUntil(), po.getLastLoginTime(), po.getLastLoginIp(), po.getPwdUpdateTime());
    }

    private UserCredentialPO toPO(UserCredential c) {
        UserCredentialPO po = new UserCredentialPO();
        po.setUserId(c.getUserId());
        po.setUsername(c.getUsername());
        po.setPasswordHash(c.getPasswordHash());
        po.setMobile(c.getMobile());
        po.setEmail(c.getEmail());
        po.setStatus(c.getStatus().name());
        po.setFailCount(c.getFailCount());
        po.setLockUntil(c.getLockUntil());
        po.setLastLoginTime(c.getLastLoginTime());
        po.setLastLoginIp(c.getLastLoginIp());
        po.setPwdUpdateTime(c.getPasswordUpdateTime());
        return po;
    }
}
