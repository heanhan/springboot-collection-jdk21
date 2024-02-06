package com.example.jpa.service;

import com.example.jpa.entity.Person;

public interface PersonService {

    /**
     * 根据person用户的id 查询 单个用户信息
     * @param id  用户标识 id
     * @return Person 用户明细  敏感数据进行脱敏
     */
    Person findPersonById(Long id);
}
