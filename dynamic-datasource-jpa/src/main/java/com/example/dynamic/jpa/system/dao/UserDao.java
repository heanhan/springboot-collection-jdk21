package com.example.dynamic.jpa.system.dao;

import com.example.dynamic.jpa.system.entity.User;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * <p>
 * 系統用戶表 Mapper 接口
 * </p>
 *
 * @author zhaojh
 */
@Repository
public interface UserDao extends BaseRepository<User,Integer>, JpaSpecificationExecutor<User> {

    /**
     * 更加用户id获取用户详细信息
     *
     * @param userId id
     * @return java.util.Map<java.lang.String, java.lang.Object>
     */
//    Map<String, Object> getUserDetailById(Integer userId);

    /**
     * 获取租户或经销商所有指定角色类型的用户数据
     *
     * @param tenantId   租户id
     * @param operatorId 经销商ID
     * @param roleType   角色类型
     * @return java.util.List<cn.greenbon.api.business.system.bean.User>
     */
//    List<User> listUserByRoleType(Integer tenantId, Integer operatorId, Integer roleType);

    /**
     * 获取租户或经销商所有指定用户类型的用户数据
     * @return java.util.List<cn.greenbon.api.business.system.bean.User>
     */
//    List<User> listUserByType(Integer tenantId, Integer operatorId, Integer userType);

    @Query(value = "select * from sys_user where username=?1 and status=?2 ",nativeQuery = true)
    User getUserByName(String username, int value);
}
