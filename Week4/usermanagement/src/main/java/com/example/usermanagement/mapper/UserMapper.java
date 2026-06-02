package com.example.usermanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.usermanagement.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
