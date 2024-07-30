package com.example.mybatis.service;

import com.example.mybatis.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.mybatis.model.dto.UserDto;
import com.example.mybatis.model.vo.SelectUserVo;
import com.example.mybatis.model.vo.UpdateUserVo;
import com.example.mybatis.model.vo.UserVo;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author zhaojh
 * @since 2024-07-12
 */
public interface UserService extends IService<User> {

    /**
     * 添加用户
     * @param userVo vo
     * @return boolean
     */
    Boolean addUser(UserVo userVo);

    /**
     *
     * @param id
     * @return
     */
    UserDto findUserById(Long id);

    /**
     * 更新用户信息
     * @param vo 参数vo
     * @return boolean
     */
    Boolean updateUserInfo(UpdateUserVo vo);

    /**
     * 删除操作 通过用户id
     * @param id 用户id
     * @return
     */
    Boolean deleteUserById(Long id);

    /**
     * 查询用户列表 带有动态多条件查询
     * @param vo
     * @return
     */
    List<User> findAllUserList(SelectUserVo vo);
}
