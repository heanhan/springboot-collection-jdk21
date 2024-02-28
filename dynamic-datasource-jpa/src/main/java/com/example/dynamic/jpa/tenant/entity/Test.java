package com.example.dynamic.jpa.tenant.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;

/**
 * <p>
 * 租户的数据库的用戶表
 * </p>
 *
 * @author zhaojh
 * @since 2022-11-04
 */
@Data
@Entity
@Table(name = "t_test")
@DynamicInsert
@DynamicUpdate
public class Test implements Serializable {

    private static final long serialVersionUID = -6367853010469956128L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name")
    private String name;

}
