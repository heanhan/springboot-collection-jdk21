package com.example.dynamic.jpa.system.config;


import com.example.dynamic.jpa.system.entity.DataSourceType;

import java.lang.annotation.*;

/**
 * @author zhaojh
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface MyDataSource {

    /**
     * 数据源key
     */
    DataSourceType type() default DataSourceType.SYSTEM;

    int value() default 0;
}
