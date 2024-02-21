package com.example.jpa.service;

import com.example.jpa.entity.Person;
import org.example.commons.result.ResultBody;

public interface PersonService {

    /**
     * 根据person用户的id 查询 单个用户信息
     * @param id  用户标识 id
     * @return Person 用户明细  敏感数据进行脱敏
     */
    Person findPersonById(Long id);

    /**
     * 添加用户
     * @return 新增的用户信息
     */
    Person addPerson(Person person);

    /**
     * 写法一：原生sql 条件为属性
     * 动态条件查询用户
     * @return
     */
    ResultBody findPersonByCondition0(Person person);

    /**
     * 写法二：原生sql 条件为对象
     * 动态条件查询用户
     * @return
     */
    ResultBody findPersonByCondition1(Person person);
}
