package com.example.mybatis.mapper;

import com.example.mybatis.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author zhaojh
 * @since 2024-07-12
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}
