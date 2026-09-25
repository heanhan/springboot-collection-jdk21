package com.example.dynamic.jpa.system.dao;

import com.example.dynamic.jpa.system.entity.AuthNode;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 权限节点数据层
 *
 * @author zhaojh
 */
@Repository
public interface AuthNodeDao extends BaseRepository<AuthNode, Integer> {

    List<AuthNode> findAllByIsDelFalseOrderByIdAsc();

    Optional<AuthNode> findByIdAndIsDelFalse(Integer id);

    boolean existsByPathAndMethodAndIsDelFalse(String path, String method);

    boolean existsByPathAndMethodIsNullAndIsDelFalse(String path);
}
