package com.example.ddd.user.infrastructure.persistence.repository;

import com.example.ddd.user.domain.model.aggregate.User;
import com.example.ddd.user.domain.model.entity.AddressEntry;
import com.example.ddd.user.domain.repository.UserRepository;
import com.example.ddd.user.infrastructure.persistence.converter.UserConverter;
import com.example.ddd.user.infrastructure.persistence.dao.UserAddressDao;
import com.example.ddd.user.infrastructure.persistence.dao.UserDao;
import com.example.ddd.user.infrastructure.persistence.dao.UserRoleDao;
import com.example.ddd.user.infrastructure.persistence.po.UserAddressPO;
import com.example.ddd.user.infrastructure.persistence.po.UserPO;
import com.example.ddd.user.infrastructure.persistence.po.UserRolePO;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 仓储实现 (Repository Implementation)：User 聚合根。
 *
 * <p><b>加载策略：</b>
 * 一次 {@link #findById} 加载完整聚合：
 * <ol>
 *   <li>t_user 主表</li>
 *   <li>t_user_address 地址集合（一对多）</li>
 *   <li>t_user_role 关联表 -> roleIds</li>
 * </ol>
 * 这里刻意不使用 JPA 的 {@code @OneToMany} 关联映射，而是手动组装，
 * 这样领域模型不受 JPA 代理机制影响，行为更可控。</p>
 *
 * <p><b>保存策略：</b>
 * <ul>
 *   <li>User 主表使用 {@code save()} upsert。</li>
 *   <li>地址表使用"全量覆盖"策略：先按 addressId 逐条 save，
 *       再软删除聚合中不存在的记录。适合学习项目；生产环境可做差量对比。</li>
 *   <li>用户角色关联表：先删后插（简单可靠）。</li>
 * </ul>
 *
 * @author ddd-learning
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserDao userDao;
    private final UserAddressDao addressDao;
    private final UserRoleDao userRoleDao;
    private final UserConverter converter;

    public UserRepositoryImpl(UserDao userDao, UserAddressDao addressDao,
                              UserRoleDao userRoleDao, UserConverter converter) {
        this.userDao = userDao;
        this.addressDao = addressDao;
        this.userRoleDao = userRoleDao;
        this.converter = converter;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(String userId) {
        Optional<UserPO> po = userDao.findByIdAndNotDeleted(userId);
        return po.map(this::assembleAggregate);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByMobile(String mobile) {
        return userDao.findByMobile(mobile).map(this::assembleAggregate);
    }

    @Override
    @Transactional
    public void save(User user) {
        // 1. 主表 upsert
        UserPO po = converter.toPO(user);
        // 保留原有 createTime / version（若存在），避免 JPA 更新时把审计字段覆盖
        userDao.findByIdAndNotDeleted(user.getUserId()).ifPresent(existing -> {
            po.setCreateTime(existing.getCreateTime());
            po.setVersion(existing.getVersion());
        });
        userDao.save(po);

        // 2. 地址表：全量覆盖（简单可靠）
        List<String> keepIds = new ArrayList<>();
        for (AddressEntry entry : user.getAddresses()) {
            UserAddressPO addrPo = converter.toAddressPO(entry);
            addressDao.findById(entry.getAddressId()).ifPresent(existing -> {
                addrPo.setCreateTime(existing.getCreateTime());
                addrPo.setVersion(existing.getVersion());
            });
            addressDao.save(addrPo);
            keepIds.add(entry.getAddressId());
        }
        if (keepIds.isEmpty()) {
            addressDao.softDeleteAllByUserId(user.getUserId());
        } else {
            addressDao.softDeleteNotIn(user.getUserId(), keepIds);
        }

        // 3. 用户角色关联：先删后插
        userRoleDao.deleteByUserId(user.getUserId());
        List<UserRolePO> relations = user.getRoleIds().stream()
                .map(roleId -> new UserRolePO(user.getUserId(), roleId))
                .toList();
        if (!relations.isEmpty()) {
            userRoleDao.saveAll(relations);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> list(int pageNum, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(0, pageNum - 1), pageSize);
        return userDao.findAllNotDeleted(pageable).getContent().stream()
                .map(this::assembleAggregate)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return userDao.countNotDeleted();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByMobile(String mobile) {
        return userDao.existsByMobileNotDeleted(mobile);
    }

    // ============================================================
    // 内部装配
    // ============================================================

    private User assembleAggregate(UserPO po) {
        List<UserAddressPO> addressPOs = addressDao.findByUserId(po.getUserId());
        List<String> roleIds = userRoleDao.findByUserId(po.getUserId()).stream()
                .map(UserRolePO::getRoleId).toList();
        return converter.toDomain(po, addressPOs, roleIds);
    }
}
