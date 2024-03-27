package com.examle.security6.dao;

import com.examle.security6.entity.AuthorityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthorityMapper extends JpaRepository<AuthorityEntity,Long> , JpaSpecificationExecutor<AuthorityEntity> {


     @Query(value = "select ta.id, ta.authority from sys_role_authority tra " +
             " left join sys_authority ta on tra.authority = ta.authority " +
             " where tra.role =?1 ",nativeQuery = true)
     List<AuthorityEntity> selectAuthorityByUsername(String username);
}
