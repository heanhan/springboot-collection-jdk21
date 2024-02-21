package com.example.jpa.service.impl;

import com.example.jpa.repository.PersonRepository;
import com.example.jpa.entity.Person;
import com.example.jpa.service.PersonService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.result.ResultBody;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class PersonServiceImpl implements PersonService {

    @Resource
    private PersonRepository personRepository;
    /**
     * 根据person用户的id 查询 单个用户信息
     *
     * @param id 用户标识 id
     * @return Person 用户明细  敏感数据进行脱敏
     */
    @Override
    public Person findPersonById(Long id) {
        Optional<Person> personOptional = personRepository.findById(id);
        if(personOptional.isPresent()){
            return personOptional.get();
        }
        return null;
    }

    /**
     * 添加用户
     *
     * @param person
     * @return 新增的用户信息
     */
    @Override
    public Person addPerson(Person person) {
        try {
            Person save = personRepository.save(person);
            Assert.notNull(save,"添加用户失败");
            return save;
        } catch (Exception e) {
            log.info("添加用户失败，报错原因：{}",e.getMessage());
        }
        return null;
    }

    /**
     * 写法一：原生sql 条件为属性
     * 动态条件查询用户
     * 用户名支持模糊查询、最近一个月更新的用户，身高大于某个值
     *
     * @param person
     * @return
     */
    @Override
    public ResultBody findPersonByCondition0(Person person) {
        List<Person> list=null;
        try {
//            list = personRepository.findPersonByCondition0(person.getUserName(),person.getHeight(),person.getLastModifiedTime());
            list = personRepository.findPersonByCondition0(person.getUserName(),person.getHeight());
        } catch (Exception e) {
            log.info("动态查询用户信息报错，明细如下：{}",e.getMessage());
            return ResultBody.error("查询出现异常");
        }
        return ResultBody.success(list);
    }


    /**
     * 写法二：原生sql 条件为对象
     * 动态条件查询用户
     *
     * @param person
     * @return
     */
    @Override
    public ResultBody findPersonByCondition1(Person person) {
        List<Person> list=null;
        try {
            list = personRepository.findPersonByCondition1(person);
        } catch (Exception e) {
            log.info("动态查询用户信息报错，明细如下：{}",e.getMessage());
            return ResultBody.error("查询出现异常");
        }
        return ResultBody.success(list);
    }
}
