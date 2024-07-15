package com.example.mybatis.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @Description : 设置mybatis-plus 自动填充的字段属性
 * @Author : zhaojh
 * @Date : 2024/7/12 15:50
 */
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {


    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("开始插入填充...");
//        List<StrictFill> list=new ArrayList<>();
//        StrictFill s=new StrictFill("createTime",LocalDateTime.class,LocalDateTime::now);
//        StrictFill s1=new StrictFill("deleted",Integer.class,() -> 1);
//        list.add(s);
//        list.add(s1);
        //方式1
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());

    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("开始更新填充...");
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
