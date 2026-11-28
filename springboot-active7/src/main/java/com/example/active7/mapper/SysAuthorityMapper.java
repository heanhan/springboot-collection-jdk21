package com.example.active7.mapper;

import com.example.active7.service.rbac.SysAuthorityRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysAuthorityMapper {

    List<String> selectRoleCodes(@Param("userId") String userId);

    List<String> selectPermissionCodes(@Param("userId") String userId);

    List<SysAuthorityRow> selectUserRoleRows();

    List<SysAuthorityRow> selectRolePermissionRows();

    int countAdminUsers();
}
