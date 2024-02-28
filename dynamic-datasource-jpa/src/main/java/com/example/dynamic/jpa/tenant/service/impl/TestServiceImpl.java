package com.example.dynamic.jpa.tenant.service.impl;


import com.example.dynamic.jpa.tenant.dao.TestDao;
import com.example.dynamic.jpa.tenant.entity.Test;
import com.example.dynamic.jpa.tenant.service.TestService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


/**
 * <p>
 * 系統用戶表 服务实现类
 * </p>
 *
 * @author zhaojh
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class TestServiceImpl implements TestService {

    @Resource
    public TestDao testDao;

    @Override
    public Test getTestById(Integer id) {
        Optional<Test> byId = (Optional<Test>) testDao.findById(id);
        if(byId.isPresent()){
            Test test = byId.get();
            return  test;
        }
        return null;
    }
}
