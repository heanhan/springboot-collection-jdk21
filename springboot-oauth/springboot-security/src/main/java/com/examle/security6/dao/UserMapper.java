package com.examle.security6.dao;

import com.examle.security6.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMapper extends JpaRepository<UserEntity,Long>, JpaSpecificationExecutor<UserEntity> {

    @Query(value = "select id, username, password from sys_user where username =?1"
            ,nativeQuery = true)
    UserEntity selectUserByUsername(String username);
}
