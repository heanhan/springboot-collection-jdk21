package com.example.jpa.controller;


import com.example.jpa.entity.Person;
import com.example.jpa.service.PersonService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.result.ResultBody;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RequestMapping(value = "/person")
@RestController
public class PersonController {

    @Resource
    private PersonService personService;

    @GetMapping("/findPersonById/{id}")
    public ResultBody findPersonById(@PathVariable(value = "id") Long id){

        Person person=personService.findPersonById(id);
        Assert.notNull(person,"没有查询到数据！");
        return ResultBody.success(person);
    }
}
