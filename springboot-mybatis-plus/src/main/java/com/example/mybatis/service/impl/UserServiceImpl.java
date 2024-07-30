package com.example.mybatis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.utils.Snowflake;
import com.example.mybatis.entity.User;
import com.example.mybatis.mapper.UserMapper;
import com.example.mybatis.model.dto.UserDto;
import com.example.mybatis.model.vo.SelectUserVo;
import com.example.mybatis.model.vo.UpdateUserVo;
import com.example.mybatis.model.vo.UserVo;
import com.example.mybatis.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    /**
     * 更新用户信息
     *
     * @param vo 参数vo
     * @return boolean
     */
    @Override
    public Boolean updateUserInfo(UpdateUserVo vo) {
        User user = new User();
        BeanUtils.copyProperties(vo,user);
        //todo 处理更新参数
//        userMapper.update()
        return null;
    }

    /**
     * 删除操作 通过用户id
     *
     * @param id 用户id
     * @return
     */
    @Override
    public Boolean deleteUserById(Long id) {
        int i = userMapper.deleteById(id);
        return i > 0;
    }

    /**
     * 查询用户列表 带有动态多条件查询
     *
     * @param vo
     * @return
     */
    @Override
    public List<User> findAllUserList(SelectUserVo vo) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        //动态处理条件
        if(vo.getId()!=null){
            queryWrapper.eq("id",vo.getId());
        }
        if(StringUtils.hasText(vo.getPhone())){
            //模糊查询 进行左匹配
            queryWrapper.likeLeft("phone",vo.getPhone());
        }
        if(StringUtils.hasText(vo.getClassName())){
            queryWrapper.eq("class_name",vo.getClassName());
        }
        List<User> users = userMapper.selectList(queryWrapper);
        users.forEach(item->{
            item.setPassword("*******");
        });
        return users;
    }
}
