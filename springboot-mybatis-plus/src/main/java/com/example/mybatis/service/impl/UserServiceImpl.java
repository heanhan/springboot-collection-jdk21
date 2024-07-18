package com.example.mybatis.service.impl;

import com.example.common.utils.Snowflake;
import com.example.mybatis.entity.User;
import com.example.mybatis.mapper.UserMapper;
import com.example.mybatis.model.dto.UserDto;
import com.example.mybatis.model.vo.UserVo;
import com.example.mybatis.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author zhaojh
 * @since 2024-07-12
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private UserMapper userMapper;
    /**
     * 添加用户
     *
     * @param userVo vo
     * @return boolean
     */
    @Override
    public Boolean addUser(UserVo userVo) {
        User user = new User();
        BeanUtils.copyProperties(userVo,user);
        return userMapper.insertOrUpdate(user);
    }

    /**
     * @param id
     * @return
     */
    @Override
    public UserDto findUserById(Long id) {
        User user = userMapper.selectById(id);
        UserDto userDto = new UserDto();
        BeanUtils.copyProperties(user,userDto);
        //处理用户密码
        userDto.setPhone("********");
        return userDto;
    }
}
