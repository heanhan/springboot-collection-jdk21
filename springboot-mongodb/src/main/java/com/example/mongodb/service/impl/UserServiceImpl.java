package com.example.mongodb.service.impl;

import com.example.mongodb.entity.User;
import com.example.mongodb.model.vo.UserVo;
import com.example.mongodb.service.UserService;
import com.example.mongodb.utils.IdWorker;
import jakarta.annotation.Resource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

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


    /**
     * 动态条件查询所有的用户
     * @param userVo 查询的条件
     * @return
     */
    @Override
    public List<User> findAllUsers(UserVo userVo) {
        Query query=new Query();
        Criteria criteria=new Criteria();
        query.addCriteria(criteria);
        List<User> users = mongoTemplate.find(query, User.class, "users");
        return users;
    }
}
