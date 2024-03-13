package com.example.mongodb.service;


import com.example.mongodb.entity.User;
import com.example.mongodb.model.vo.UserVo;

import java.util.List;

public interface UserService {

    /**
     * 添加测试数据- 用户
     * @return
     */
    Boolean addUserTestData();

    /**
     * 动态条件查询所有的用户
     * @param userVo 查询的条件
     * @return
     */
    List<User> findAllUsers(UserVo userVo);
}
