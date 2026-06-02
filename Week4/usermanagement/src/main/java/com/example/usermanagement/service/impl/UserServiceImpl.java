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
        return userMapper.findAll();
    }

    @Override
    public User getById(Integer id) {
        User user=userMapper.findById(id);
        if(user==null){throw new BusinessException(404, "用户不存在，id="+id);}
        return user;
    }

    @Override
    public User create(User user) {
        userMapper.insert(user);
        return user;
    }

    @Override
    public User update(User user) {
        getById(user.getId()); // 确保用户存在
        userMapper.update(user);
        return user;
    }

    @Override
    public void deleteById(Integer id) {
        getById(id); // 不存在则抛出异常
        userMapper.deleteById(id);
    }
}
