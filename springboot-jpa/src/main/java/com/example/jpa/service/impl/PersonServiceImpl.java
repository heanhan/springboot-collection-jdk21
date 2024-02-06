package com.example.jpa.service.impl;

import com.example.jpa.repository.PersonRepository;
import com.example.jpa.entity.Person;
import com.example.jpa.service.PersonService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PersonServiceImpl implements PersonService {

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
        return null;
    }
}
