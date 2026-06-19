package com.example.usermanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.usermanagement.dto.UserUpdateDTO;
import com.example.usermanagement.entity.Role;
import com.example.usermanagement.entity.User;
import com.example.usermanagement.entity.UserRole;
import com.example.usermanagement.exception.BusinessException;
import com.example.usermanagement.mapper.RoleMapper;
import com.example.usermanagement.mapper.UserMapper;
import com.example.usermanagement.mapper.UserRoleMapper;
import com.example.usermanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /* 为单个User对象填充角色列表 */
    private void fillRoles(User user) {
        if(user==null) return;
        // 查询用户关联的角色ID
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRole::getUserId, user.getId());
        List<UserRole> userRoles = userRoleMapper.selectList(wrapper);
        if (userRoles.isEmpty()) {
            user.setRoles(List.of());
            return;
        }
        // 查询角色信息
        List<Integer> roleIds = userRoles.stream().map(UserRole::getRoleId).toList();
        List<Role> roles = roleMapper.selectBatchIds(roleIds);
        user.setRoles(roles);
    }

    @Override
    public List<User> listAll() {
        // Mybatis-Plus 查询所有
        List<User> users = userMapper.selectList(null);
        // 为每个用户填充角色列表
        users.forEach(this::fillRoles);
        return users;
    }

    @Override
    @Cacheable(value = "user", key="#id", unless="#result==null")
    // 执行方法前先查缓存，key为“user::”+id，如果缓存存在则直接返回；否则执行方法并将返回值存入缓存。如果结果为 null，则不缓存
    public User getById(Integer id) {
        User user=userMapper.selectById(id);
        if(user==null){throw new BusinessException(404, "用户不存在，id="+id);}
        fillRoles(user);
        return user;
    }

    @Override
    @Transactional
    public User create(User user) {
        // Mybatis-Plus 插入，会自动将生成的自增 id 设置回 user 对象
        // 1. 如果没有密码，设置默认密码并加密
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode("123456"));
        }
        // 2. 如果没有创建时间，设置为当前时间
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }
        // 3. 插入数据库
        userMapper.insert(user);
        // 4. 分配默认角色 ROLE_USER（如果不存在则创建）
        Role defaultRole = getRoleOrThrow("ROLE_USER");
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(defaultRole.getId());
        userRoleMapper.insert(userRole);

        return user;
    }

    /**
     * 根据角色名获取角色，如果不存在则报错
     */
    private Role getRoleOrThrow(String roleName) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getName, roleName);
        Role role = roleMapper.selectOne(wrapper);
        if (role == null) {
            throw new BusinessException(404, "角色不存在：" + roleName);
        }
        return role;
    }

    @Override
    @CacheEvict(value = "user", key="#id")
    @Transactional
    public User update(Integer id, UserUpdateDTO dto) {
        User existingUser = getById(id); // 确保用户存在
        // 仅复制非null字段（防止更新时null字段覆盖原值）
        if (dto.getUsername() != null) existingUser.setUsername(dto.getUsername());
        if (dto.getEmail() != null) existingUser.setEmail(dto.getEmail());
        if (dto.getAge() != null) existingUser.setAge(dto.getAge());
        // 更新数据库
        userMapper.updateById(existingUser);
        return existingUser;
    }

    /*
     * update() & deleteById() 缓存自调用警告：
     * 调用了getById()-->内部调用this.getById()
     * --> Spring无法拦截（Spring缓存基于AOP代理实现，只有通过代理对象调用的方法才会应用缓存逻辑，而类内部直接通过this调用不会触发代理）
     * --> 缓存不会生效：不会先查缓存，而是直接执行方法体，每次都访问数据库（存在性校验直接查数据库）
     * 【存在性校验逻辑单独抽取成一个私有方法？】
     */
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
        Page<User> result = userMapper.selectPage(pageObj, queryWrapper);
        result.getRecords().forEach(this::fillRoles);
        return result;
    }

    @Override
    public User findByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    @Override
    @Transactional
    /*
     * Tannsactional: register内两步数据库操作：先 userMapper.insert(user) 再 userRoleMapper.insert(ur)
     * 如果第二步失败了（如角色表找不到 ROLE_USER），第一步的 user 已经入库了，但这个用户没有角色，后续登录会出权限问题。数据库脏数据很难排查。
     */
    public User register(User user) {
        // 1. 检查用户名是否已存在
        if (findByUsername(user.getUsername()) != null) {
            throw new BusinessException(400, "用户名已存在");
        }
        // 2. 密码加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // 3. 插入用户
        userMapper.insert(user);
        // 4. 分配默认角色（ROLE_USER）
        LambdaQueryWrapper<Role> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.eq(Role::getName, "ROLE_USER");
        Role userRole = roleMapper.selectOne(roleWrapper);
        if (userRole != null) {
            UserRole ur = new UserRole();
            ur.setUserId(user.getId());
            ur.setRoleId(userRole.getId());
            userRoleMapper.insert(ur);
        }
        return user;
    }
}
