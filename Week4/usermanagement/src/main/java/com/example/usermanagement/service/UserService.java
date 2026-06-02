package com.example.usermanagement.service;

import com.example.usermanagement.entity.User;
import java.util.List;

public interface UserService {
    List<User> listAll();
    User getById(Integer id);
    User create(User user);
    User update(User user);
    void deleteById(Integer id);
}
