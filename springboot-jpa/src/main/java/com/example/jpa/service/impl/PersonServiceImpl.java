package com.example.jpa.service.impl;

import com.example.jpa.dto.PersonDto;
import com.example.jpa.dto.PersonWorkDto;
import com.example.jpa.entity.Person;
import com.example.jpa.repository.PersonRepository;
import com.example.jpa.service.PersonService;
import com.example.jpa.vo.PersonExtraVo;
import com.example.jpa.vo.PersonSelectParam;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.result.ResultBody;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    @Transactional
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

    @Override
    public Page<Person> findPersonPageByCondition(PersonSelectParam param) {
        //封装分页参数
        Pageable pageable = PageRequest.of(param.getIndexSize(), param.getPageSize(), Sort.Direction.ASC, "lastModifiedTime");
        //动态多条件的封装查询参数
        Specification<Person> spec=new Specification<Person>() {
            @Override
            public Predicate toPredicate(Root<Person> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                /**
                 * root 拿到当前对象的所有信息
                 * Path 包装 从实体中获取的属性
                 * Predicate 为属赋值的对象
                 */
                //1、创建一个Prddicate 添加封装集合
                List<Predicate> predicateList=new ArrayList<>();
                //姓名的查询
                String userName = param.getUserName();
                Long id = param.getId();
                Boolean sex = param.getSex();
                LocalDateTime lastModifiedTime = param.getLastModifiedTime();
                if(userName!=null&&!"".equals(userName)){
                    Path<Person> c1 = root.get("userName");
//                    predicateList.add(criteriaBuilder.equal(c1.as(Integer.class), userName));//精确查询
                    //模糊查询
                    predicateList.add(criteriaBuilder.like(c1.as(String.class), "%"+userName+"%"));
                }
                //id
                if(id!=null){
                    Path<Person> c2 = root.get("id");
                    predicateList.add(criteriaBuilder.equal(c2.as(Long.class),id));
                }
                //性别
                Optional<Boolean> sex1 = Optional.ofNullable(sex);
                if(sex1.isPresent()){
                    Path<Person> c3 = root.get("sex");
                    predicateList.add(criteriaBuilder.equal(c3.as(Boolean.class),sex));
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };
        Page<Person> all = personRepository.findAll(spec, pageable);
        return all;
    }

    /**
     * 多条件的分页查询  使用原生的sql
     *
     * @param param
     * @return Page<Person>
     */
    @Override
    public Page<Person> findPersonPageBySql(PersonSelectParam param) {
        //分页参数 才请求是使用validate默认已经校验
        Pageable pageable=PageRequest.of(param.getIndexSize(),param.getPageSize());
        Page<Person> all=personRepository.findPersonPageBySql(param,pageable);
        return all;
    }

    /**
     * 条件查询  返回自定义的vo
     *
     * @param param
     * @return
     */
    @Override
    public List<PersonDto> findPersonByCondition(PersonSelectParam param) {
        List<PersonDto> list=personRepository.findPersonByCondition(param);
        return list;
    }

    /**
     * 更新 数据
     *
     * @param person
     * @return
     */
    @Transactional
    @Override
    public Person updatePerson(Person person) {
        Person save = personRepository.save(person);
        return save;
    }

    /**
     * 删除根据id
     *
     * @param id
     */
    @Transactional
    @Override
    public void deletePerson(Long id) {
        personRepository.deleteById(id);
    }

    /**
     * 连表查询
     */
    @Override
    public List<PersonWorkDto> findPersonExtraInfo(PersonExtraVo vo) {
        List<PersonWorkDto> list=personRepository.findPersonExtraInfo(vo.getPersonId(),vo.getUserName(),vo.getWorker(),vo.getBeginWorkTime());
        return list;
    }

}
