package com.example.jpa.controller;


import com.example.jpa.entity.Person;
import com.example.jpa.service.PersonService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.result.ResultBody;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RequestMapping(value = "/person")
@RestController
public class PersonController {

    @Resource
    private PersonService personService;

    /**
     * 添加用户
     * @param person
     * @return
     */
    @PostMapping(value = "/addPerson")
    public ResultBody addPerson(@RequestBody Person person){
        Person obj= personService.addPerson(person);
        if(ObjectUtils.isEmpty(obj)){
            return ResultBody.error("添加用户失败");
        }
        return ResultBody.success();
    }

    /**
     * 根据用户id查询用户
     * @param id
     * @return
     */
    @GetMapping("/findPersonById/{id}")
    public ResultBody findPersonById(@PathVariable(value = "id") Long id){
        Person person=personService.findPersonById(id);
        Assert.notNull(person,"没有查询到数据！");
        return ResultBody.success(person);
    }

    /**
     * 写法二：原生sql 条件为对象
     * 动态条件查询用户
     * @return
     */
    @PostMapping(value = "/findPersonByCondition0")
    public ResultBody findPersonByCondition0(@RequestBody Person person){
        return personService.findPersonByCondition0(person);
    }

    /**
     * 写法二：原生sql 条件为对象
     * 动态条件查询用户
     * @return
     */
    @PostMapping(value = "/findPersonByCondition1")
    public ResultBody findPersonByCondition1(@RequestBody Person person){
        return personService.findPersonByCondition1(person);
    }
}
