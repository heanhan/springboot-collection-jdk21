package com.example.jpa.service;

import com.example.jpa.dto.PersonDto;
import com.example.jpa.dto.PersonWorkDto;
import com.example.jpa.entity.Person;
import com.example.jpa.vo.PersonExtraVo;
import com.example.jpa.vo.PersonSelectParam;
import org.example.commons.result.ResultBody;
import org.springframework.data.domain.Page;

import java.util.List;

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

    /**
     * 多条件的分页查询
     * @param param
     * @return  Page<Person>
     */
    Page<Person> findPersonPageByCondition(PersonSelectParam param);


    /**
     * 多条件的分页查询  使用原生的sql
     * @param param
     * @return  Page<Person>
     */
    Page<Person> findPersonPageBySql(PersonSelectParam param);

    /**
     * 条件查询  返回自定义的vo
     * @param param
     * @return
     */
    List<PersonDto> findPersonByCondition(PersonSelectParam param);


    /**
     * 更新 数据
     * @param person
     * @return
     */
    Person updatePerson(Person person);

    /**
     * 删除根据id
     * @param id
     */
    void deletePerson(Long id);

    /**
     * 连表查询
     */
    List<PersonWorkDto> findPersonExtraInfo(PersonExtraVo personExtraVo);
}
