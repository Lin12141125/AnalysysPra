package com.example.usermanagement.service.impl;

import com.example.usermanagement.common.ResultCode;
import com.example.usermanagement.entity.User;
import com.example.usermanagement.exception.BusinessException;
import com.example.usermanagement.mapper.UserMapper;
import com.example.usermanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<User> listAll() {
        // Mybatis-Plus 查询所有
        return userMapper.selectList( null);
    }

    @Override
    public User getById(Integer id) {
        User user=userMapper.selectById(id);
        if(user==null){throw new BusinessException(404, "用户不存在，id="+id);}
        return user;
    }

    @Override
    public User create(User user) {
        // Mybatis-Plus 插入，会自动将生成的自增 id 设置回 user 对象
        userMapper.insert(user);
        return user;
    }

    @Override
    public User update(User user) {
        getById(user.getId()); // 确保用户存在
        // MP 根据 id 更新，只更新非 null 字段（如果某些字段为 null，会设置为 null）
        userMapper.updateById(user);
        return user;
    }

    @Override
    public void deleteById(Integer id) {
        getById(id); // 不存在则抛出异常
        userMapper.deleteById(id);
    }
}
