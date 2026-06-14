package com.example.usermanagement.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.usermanagement.entity.Role;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

}
