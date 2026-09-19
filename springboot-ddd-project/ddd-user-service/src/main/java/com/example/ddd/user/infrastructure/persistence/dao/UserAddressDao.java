package com.example.ddd.user.infrastructure.persistence.dao;

import com.example.ddd.user.infrastructure.persistence.po.UserAddressPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * DAO：收货地址。
 */
public interface UserAddressDao extends JpaRepository<UserAddressPO, String> {

    @Query("SELECT a FROM UserAddressPO a WHERE a.userId = :userId AND a.deleted = 0 ORDER BY a.isDefault DESC, a.createTime ASC")
    List<UserAddressPO> findByUserId(@Param("userId") String userId);

    /**
     * 软删除：把该用户下"不在给定 addressIds 集合"的地址全部标记为已删除。
     * <p>用于聚合根保存时同步差量删除。</p>
     */
    @Modifying
    @Query("UPDATE UserAddressPO a SET a.deleted = 1, a.updateTime = CURRENT_TIMESTAMP " +
            "WHERE a.userId = :userId AND a.deleted = 0 AND a.addressId NOT IN :keepIds")
    int softDeleteNotIn(@Param("userId") String userId, @Param("keepIds") List<String> keepIds);

    @Modifying
    @Query("UPDATE UserAddressPO a SET a.deleted = 1, a.updateTime = CURRENT_TIMESTAMP " +
            "WHERE a.userId = :userId AND a.deleted = 0")
    int softDeleteAllByUserId(@Param("userId") String userId);
}
