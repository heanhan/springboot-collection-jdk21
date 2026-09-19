package com.example.ddd.user.domain.repository;

import com.example.ddd.user.domain.model.aggregate.User;

import java.util.List;
import java.util.Optional;

/**
 * 仓储接口 (Repository)：User 聚合根。
 *
 * <p><b>为什么仓储接口放在 domain 层？</b>
 * 这是 DDD 中<b>依赖倒置原则 (DIP)</b> 的经典应用：
 * <ul>
 *   <li>领域层定义"我需要一个能持久化 User 的东西"（接口）。</li>
 *   <li>基础设施层提供具体实现（JPA / MyBatis / MongoDB 都可以）。</li>
 *   <li>领域层不依赖基础设施，反而基础设施依赖领域层。</li>
 * </ul>
 * 这样做的好处：领域逻辑可以在没有数据库的单元测试中运行；未来切换持久化技术不影响业务代码。</p>
 *
 * <p><b>为什么以聚合为单位而不是每张表一个仓储？</b>
 * 仓储的粒度必须与聚合一致，一次 {@link #save(User)} 要把整个聚合（含所有 AddressEntry）持久化，
 * 保证聚合的一致性边界。</p>
 *
 * @author ddd-learning
 */
public interface UserRepository {

    /**
     * 根据 userId 加载完整聚合（含地址、角色 ID）。
     *
     * @return 找不到返回 Optional.empty()
     */
    Optional<User> findById(String userId);

    /**
     * 根据手机号查找用户（注册时防重复）。
     */
    Optional<User> findByMobile(String mobile);

    /**
     * 保存聚合（新增或更新）。
     * <p>实现需处理：
     * <ol>
     *   <li>User 主表 upsert</li>
     *   <li>AddressEntry 差量更新（新增/修改/删除）</li>
     *   <li>t_user_role 关联更新</li>
     * </ol>
     */
    void save(User user);

    /**
     * 分页查询用户列表（管理端）。
     */
    List<User> list(int pageNum, int pageSize);

    /**
     * 统计用户总数。
     */
    long count();

    /**
     * 判断手机号是否已存在（注册前校验，避免加载整个聚合）。
     */
    boolean existsByMobile(String mobile);
}
