package com.example.usermanagement.service;

import com.example.usermanagement.dto.UserUpdateDTO;
import com.example.usermanagement.entity.User;
import java.util.List;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface UserService {
    List<User> listAll();
    User getById(Integer id);
    User create(User user);
    User update(Integer id, UserUpdateDTO dto);
    void deleteById(Integer id);
    Page<User> pageQuery(int page, int size, String keyword);

    User findByUsername(String username);
    User register(User user); // 注册用户（默认分配ROLE_USER角色）
}
