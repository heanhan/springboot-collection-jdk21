package com.example.ddd.common.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

/**
 * JPA 通用可审计 PO 抽象基类。
 *
 * <p><b>为什么单独抽出一个基类？</b>
 * 所有 PO 都有一些公共字段（创建时间、更新时间、乐观锁版本号、软删除标记），
 * 抽出来避免重复代码。使用 {@code @MappedSuperclass} 而不是 {@code @Entity}，
 * 表示这个类不映射为独立表，只是提供字段给子类继承。</p>
 *
 * <p><b>DDD 中的 PO 是什么？</b>
 * PO (Persistent Object) 是<b>基础设施层</b>的对象，专用于数据库映射。
 * 领域模型 (Aggregate Root / Entity / Value Object) 位于<b>领域层</b>，
 * 二者通过 Converter 双向转换，这样做的好处：
 * <ul>
 *   <li>领域模型不受 JPA 注解污染，保持纯粹的业务语义。</li>
 *   <li>数据库表结构变化时，只需修改 PO 和 Converter，不影响领域逻辑。</li>
 *   <li>可以为不同聚合选择不同持久化技术（例如商品用 ES、订单用 MySQL）。</li>
 * </ul>
 *
 * @author ddd-learning
 */
@MappedSuperclass
public abstract class AbstractJpaAuditablePO {

    /** 创建时间，插入前自动填充 */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间，更新前自动填充 */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * 乐观锁版本号。
     * <p>JPA 会在 UPDATE 语句中自动加入 {@code WHERE version = ?}，
     * 并在新版本中把 version + 1，冲突时抛 {@code OptimisticLockException}。</p>
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * 软删除标记：0 = 未删除，1 = 已删除。
     * <p>业务查询默认加 {@code WHERE deleted = 0}；
     * 物理删除仅用于合规清理场景。</p>
     */
    @Column(name = "deleted", nullable = false, columnDefinition = "TINYINT")
    private Integer deleted = 0;

    /**
     * JPA 生命周期回调：插入前自动填充审计字段。
     */
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createTime == null) {
            this.createTime = now;
        }
        if (this.updateTime == null) {
            this.updateTime = now;
        }
        if (this.version == null) {
            this.version = 0L;
        }
        if (this.deleted == null) {
            this.deleted = 0;
        }
    }

    /**
     * JPA 生命周期回调：更新前刷新 updateTime。
     */
    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }

    // ============================================================
    // Getters / Setters
    // ============================================================

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
