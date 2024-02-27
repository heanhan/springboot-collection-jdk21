package com.example.jpa.controller;


import com.example.jpa.dto.PersonDto;
import com.example.jpa.dto.PersonWorkDto;
import com.example.jpa.entity.Person;
import com.example.jpa.service.PersonService;
import com.example.jpa.vo.PersonExtraVo;
import com.example.jpa.vo.PersonSelectParam;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.enums.CommonEnum;
import org.example.commons.result.ResultBody;
import org.springframework.data.domain.Page;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


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

    /**
     * 分页查询 （使用jpa的接口方法）
     * @param param
     * @return
     */
    @PostMapping(value = "/findPersonPageByCondition")
    public ResultBody findPersonPageByCondition(@RequestBody PersonSelectParam param){
        Page<Person> page = null;
        try {
            page = personService.findPersonPageByCondition(param);
        } catch (Exception e) {
            log.info("分页查询-->捕获的异常信息：{}",e.getMessage());
            return ResultBody.error(CommonEnum.INTERNAL_SERVER_ERROR);
        }
        return ResultBody.success(page);
    }

    /**
     * 分页查询 （使用原生的sql）
     * @param param
     * @return
     */
    @PostMapping(value = "/findPersonPageBySql")
    public ResultBody findPersonPageBySql(@RequestBody PersonSelectParam param){
        Page<Person> page = null;
        try {
            page = personService.findPersonPageBySql(param);
        } catch (Exception e) {
            log.info("分页查询(原生sql)-->捕获的异常信息：{}",e.getMessage());
            return ResultBody.error(CommonEnum.INTERNAL_SERVER_ERROR);
        }
        return ResultBody.success(page);
    }

    /**
     * 使用 HQL 语法的方式查询
     * @param param
     * @return
     */
    @PostMapping(value = "/findPersonByCondition")
    public ResultBody findPersonByCondition(@RequestBody PersonSelectParam param){
        List<PersonDto> list = null;
        try {
            list = personService.findPersonByCondition(param);
        } catch (Exception e) {
            log.info("报错信息：{}",e.getMessage());
            return ResultBody.error(CommonEnum.INTERNAL_SERVER_ERROR);
        }
        return ResultBody.success(list);
    }

    /**
     * 使用原生sql进行插入
     * @return
     */
    @PostMapping(value = "/updatePerson")
    public ResultBody updatePerson(@RequestBody Person person){
        try {
            personService.updatePerson(person);
        } catch (Exception e) {
            log.info("更新实体报错信息：{}",e.getMessage());
            return ResultBody.error(CommonEnum.INTERNAL_SERVER_ERROR);
        }
        return ResultBody.success(CommonEnum.SUCCESS);
    }

    /**
     * 根据id进行删除
     * @param id
     * @return
     */
    @DeleteMapping(value = "/deletePerson/{id}")
    public ResultBody deletePerson(@PathVariable("id") Long id){
        try {
            personService.deletePerson(id);
        } catch (Exception e) {
            log.info("根据id删除报错信息：删除id{}-的报错{}",id,e.getMessage());
            return ResultBody.error(CommonEnum.INTERNAL_SERVER_ERROR);
        }
        return ResultBody.success();
    }

    /**
     * 连表查询  个人的工作经历
     */
    @PostMapping(value = "/findPersonExtraInfo")
    public ResultBody findPersonExtraInfo(@RequestBody PersonExtraVo personExtraVo){
        List<PersonWorkDto> list=null;
        try {
            list=personService.findPersonExtraInfo(personExtraVo);
        } catch (Exception e) {
            log.info("联表查询报错，报错信息为：{}",e.getMessage());
            return ResultBody.error(CommonEnum.INTERNAL_SERVER_ERROR);
        }
        return ResultBody.success(list);
    }
}
