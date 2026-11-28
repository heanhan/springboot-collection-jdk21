package com.example.dynamic.jpa.system.dao;

import com.example.dynamic.jpa.system.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleDao extends JpaRepository<UserRole, Integer> {

    List<UserRole> findByUserId(Integer userId);

    List<UserRole> findByUserIdIn(List<Integer> userIds);

    List<UserRole> findByRoleId(Integer roleId);

    void deleteByUserId(Integer userId);

    boolean existsByUserIdAndRoleId(Integer userId, Integer roleId);

    long countByRoleId(Integer roleId);
}
