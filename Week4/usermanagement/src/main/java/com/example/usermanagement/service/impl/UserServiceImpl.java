package com.example.usermanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.usermanagement.entity.User;
import com.example.usermanagement.exception.BusinessException;
import com.example.usermanagement.mapper.UserMapper;
import com.example.usermanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    @Cacheable(value = "user", key="#id", unless="#result==null")
    // 执行方法前先查缓存，key为“user::”+id，如果缓存存在则直接返回；否则执行方法并将返回值存入缓存。如果结果为 null，则不缓存
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
    @CacheEvict(value = "user", key="#user.id")
    public User update(User user) {
        getById(user.getId()); // 确保用户存在
        // MP 根据 id 更新，只更新非 null 字段（如果某些字段为 null，会设置为 null）
        userMapper.updateById(user);
        return user;
    }

    @Override
    @CacheEvict(value = "user", key="#id")
    public void deleteById(Integer id) {
        getById(id); // 不存在则抛出异常
        userMapper.deleteById(id);
    }

    @Override
    public Page<User> pageQuery(int page, int size, String keyword) {
        Page<User> pageObj=new Page<>(page, size);
        LambdaQueryWrapper<User> queryWrapper=new LambdaQueryWrapper<>();
        if (keyword!=null && !keyword.trim().isEmpty()) queryWrapper.like(User::getUsername, keyword);
        queryWrapper.orderByDesc(User::getCreatedAt); //按创建时间倒序
        return userMapper.selectPage(pageObj, queryWrapper);
    }
}
