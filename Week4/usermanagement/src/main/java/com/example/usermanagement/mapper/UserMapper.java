package com.example.usermanagement.mapper;

import com.example.usermanagement.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface UserMapper {
    List<User> findAll();
    User findById(@Param("id") Integer id);
    int insert(User user);
    int update(User user);
    int deleteById(@Param("id") Integer id);
}
