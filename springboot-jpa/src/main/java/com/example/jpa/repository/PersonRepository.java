package com.example.jpa.repository;

import com.example.jpa.model.dto.PersonDto;
import com.example.jpa.model.dto.PersonWorkDto;
import com.example.jpa.entity.Person;
import com.example.jpa.model.vo.PersonSelectParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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


    @Query(value="select new com.example.jpa.dto.PersonDto(p.id,p.userName, p.sex, p.height, p.lastModifiedTime, p.lastModifierId, p.isDeleted) from Person as p" +
            " where p.isDeleted=false" +
            " AND (:#{#param.getUserName()} is null or p.userName LIKE %:#{#param.getUserName()}%) "
            ,nativeQuery = false)
    List<PersonDto> findPersonByCondition(@Param("param") PersonSelectParam param);


    /**
     * 查询用户的工作经历
     * @param personId  用户id
     * @param userName 用户名
     * @param worker  工作
     * @param beginWorkTime 工作开始时间
     * @return
     */
    @Query(value = "select new com.example.jpa.dto.PersonWorkDto(p.id, p.userName, p.sex, p.selfInfo, w.id, w.workName, w.workLevel, w.workContent) " +
            " from Person p , WorkInfo  w " +
            " where p.id=w.personId" +
            " and p.isDeleted =false  " +
            " and w.isDeleted=false " +
            " and ( ?1 is null or p.id=?1 ) " +
            " and ( ?2 is null or p.userName like %?2%)" +
            " and ( ?3 is null or w.workName like %?3%)" +
            " and ( ?4 is null or w.firstWorkTime >= ?4)"
    )
    List<PersonWorkDto> findPersonExtraInfo(Long personId, String userName, String worker, LocalDateTime beginWorkTime);
}
