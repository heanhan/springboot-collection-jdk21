package com.example.ddd.user.infrastructure.persistence.dao;

import com.example.ddd.user.infrastructure.persistence.po.UserPO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Spring Data JPA DAO：用户表。
 *
 * <p><b>为什么 DAO 位于 infrastructure 而不是 domain？</b>
 * DAO 直接绑定 JPA / Spring Data，是"技术细节"。
 * domain 层通过 {@link com.example.ddd.user.domain.repository.UserRepository} 接口反向依赖 DAO 的能力。</p>
 */
public interface UserDao extends JpaRepository<UserPO, String> {

    /** 手机号唯一性检查（考虑软删除） */
    @Query("SELECT u FROM UserPO u WHERE u.mobile = :mobile AND u.deleted = 0")
    Optional<UserPO> findByMobile(@Param("mobile") String mobile);

    @Query("SELECT u FROM UserPO u WHERE u.userId = :userId AND u.deleted = 0")
    Optional<UserPO> findByIdAndNotDeleted(@Param("userId") String userId);

    @Query("SELECT COUNT(u) > 0 FROM UserPO u WHERE u.mobile = :mobile AND u.deleted = 0")
    boolean existsByMobileNotDeleted(@Param("mobile") String mobile);

    @Query("SELECT u FROM UserPO u WHERE u.deleted = 0 ORDER BY u.createTime DESC")
    Page<UserPO> findAllNotDeleted(Pageable pageable);

    @Query("SELECT COUNT(u) FROM UserPO u WHERE u.deleted = 0")
    long countNotDeleted();
}
