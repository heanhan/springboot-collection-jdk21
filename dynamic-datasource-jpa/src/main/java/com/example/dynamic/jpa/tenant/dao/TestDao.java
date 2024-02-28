package com.example.dynamic.jpa.tenant.dao;

import com.example.dynamic.jpa.system.dao.BaseRepository;
import com.example.dynamic.jpa.tenant.entity.Test;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * <p>
 * 系統用戶表 Mapper 接口
 * </p>
 *
 * @author zhaojh
 * @since 2022-11-04
 */
@Repository
public interface TestDao extends BaseRepository<Test,Integer>, JpaSpecificationExecutor<Test> {


}
