package com.example.mybatis.service;

import com.example.mybatis.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.mybatis.model.dto.UserDto;
import com.example.mybatis.model.vo.UserVo;

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
}
