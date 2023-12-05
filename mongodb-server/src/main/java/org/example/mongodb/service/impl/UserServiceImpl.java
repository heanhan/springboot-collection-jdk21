package org.example.mongodb.service.impl;

import jakarta.annotation.Resource;
import org.example.mongodb.entity.User;
import org.example.mongodb.service.UserService;
import org.example.mongodb.utils.IdWorker;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private IdWorker idWorker;


    /**
     * 添加测试数据- 用户
     *
     * @return
     */
    @Override
    public Boolean addUserTestData() {
//        mongoTemplate.executeCommand()
        User user=null;

        return null;
    }
}
