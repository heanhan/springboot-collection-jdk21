package com.example.jpa.repository;

import com.example.jpa.dto.PersonDto;
import com.example.jpa.entity.Person;
import com.example.jpa.vo.PersonSelectParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long>, JpaSpecificationExecutor<Person> {


    /**
     * 写法一：原生sql 条件为属性
     *
     * 动态条件查询用户列表
     * nativeQuery =true 走纯原生sql ,false 走jpql语法
     * @param userName 用户名 支持模糊查询
     * @param height 身高 大于传参
//     * @param lastModifiedTime  当前时间
     * @return List<Person>
     */
    @Query(value = "select p.id, p.user_name, p.sex, p.height, p.hover, p.self_info, p.creator_id,p.last_modifier_id, p.last_modified_time,p.created_time,p.is_deleted,p.password from t_person p " +
            " where p.is_deleted=false " +
            " and if(?1!='',p.user_name like concat(\"%\",?1,\"%\"),1=1) " +
            " and if(?2!='',p.height>?2,1=1) "
            ,nativeQuery = true)
//    List<Person> findPersonByCondition0(@Param("userName") String userName, @Param("height") Double height, @Param("lastModifiedTime") LocalDateTime lastModifiedTime);
    List<Person> findPersonByCondition0(String userName,Double height );


    /**
     * 写法二：原生sql 条件为对象
     *
     * 动态条件查询用户列表
     * nativeQuery =true 走纯原生sql ,false 走jpql语法
     * @param person
     * @return
     */
    @Query(value = "select * from t_person p " +
            " where" +
            " if(:#{#person.userName} !='',p.user_name=:#{#person.userName},1=1) ",nativeQuery = true)
    List<Person> findPersonByCondition1(@Param(value = "person") Person person);

    @Query(value = "select p.* from t_person p  where p.is_deleted=false" +
            " and   if(:#{#param.userName} !='',p.user_name like concat(\"%\",:#{#param.getUserName()},\"%\"),1=1) " +
            " order by p.last_modified_time "
            ,nativeQuery = true)
    Page<Person> findPersonPageBySql(@Param("param") PersonSelectParam param, Pageable pageable);


    @Query(value="select p from Person as p ",nativeQuery = false)
    List<PersonDto> findPersonByCondition();
}
