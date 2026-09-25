package com.example.dynamic.jpa.system.dao;

import com.example.dynamic.jpa.system.entity.Menu;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * <p>
 * 菜单表 Mapper 接口
 * </p>
 *
 * @author zhaojh
 */

@Repository
public interface MenuDao extends BaseRepository<Menu,Integer>, JpaSpecificationExecutor<Menu> {

    List<Menu> findAllByIsDelFalseOrderByListOrderAscIdAsc();

    List<Menu> findAllByParentIdAndIsDelFalseOrderByListOrderAscIdAsc(Integer parentId);

    Optional<Menu> findByIdAndIsDelFalse(Integer id);

    boolean existsByParentIdAndIsDelFalse(Integer parentId);
}
