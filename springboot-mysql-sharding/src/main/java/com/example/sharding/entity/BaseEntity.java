package com.example.sharding.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据库实体基类，声明数据库实体类共有属性
 * @MappedSuperclass 表示一个类是某些实体类的超类，它不会直接映射为数据库表，但是可以被其他实体类继承，它的子类映射的数据库表中会包含它自身声明的属性。
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity implements Serializable {
    /**
     * ID，唯一标识列，使用主键自增策略
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 创建时间
     */
    @CreatedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    /**
     * 最后修改时间
     */
    @LastModifiedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastModifiedTime;

    /**
     * 创建人ID
     */
    private Long creatorId;

    /**
     * 最后修改人ID
     */
    private Long lastModifierId;

    /**
     * false:0 无效
     * true:1 有效 默认
     */
    private Boolean isDeleted;
}


