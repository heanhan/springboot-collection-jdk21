package com.example.oauth.service;

import com.example.oauth.dto.ChangePasswordDTO;
import com.example.oauth.dto.ProfileUpdateDTO;
import com.example.oauth.dto.RegisterDTO;
import com.example.oauth.entity.SysUser;
import com.example.oauth.vo.MenuVO;
import com.example.oauth.vo.UserInfoVO;

import java.util.List;

/**
 * 用户业务服务接口
 */
public interface UserService {

    /**
     * 用户自助注册（默认绑定 USER 角色）
     */
    SysUser register(RegisterDTO dto);

    /**
     * 用户自助修改资料
     */
    SysUser updateProfile(Long userId, ProfileUpdateDTO dto);

    /**
     * 用户自助修改密码（校验旧密码）
     */
    void changePassword(Long userId, ChangePasswordDTO dto);

    /**
     * 根据用户名查询用户（含角色、权限）
     */
    SysUser findByUsername(String username);

    /**
     * 根据 ID 查询用户
     */
    SysUser findById(Long id);

    /**
     * 构建用户信息 VO
     */
    UserInfoVO buildUserInfoVO(SysUser sysUser);

    /**
     * 获取用户菜单树
     */
    List<MenuVO> getUserMenuTree(Long userId);
}
