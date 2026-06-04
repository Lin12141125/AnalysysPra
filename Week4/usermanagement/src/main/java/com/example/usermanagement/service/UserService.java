package com.example.usermanagement.service;

import com.example.usermanagement.entity.User;
import java.util.List;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface UserService {
    List<User> listAll();
    User getById(Integer id);
    User create(User user);
    User update(User user);
    void deleteById(Integer id);
    Page<User> pageQuery(int page, int size, String keyword);
}
