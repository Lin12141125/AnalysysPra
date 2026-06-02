package com.example.usermanagement.controller;

import com.example.usermanagement.common.Result;
import com.example.usermanagement.dto.UserCreateDTO;
import com.example.usermanagement.dto.UserUpdateDTO;
import com.example.usermanagement.entity.User;
import com.example.usermanagement.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {

    @Autowired
    private UserService userService;

    // GET /api/users 查询所有用户
    @GetMapping
    public Result<List<User>> list() {
        List<User> users = userService.listAll();
        return Result.success(users);
    }

    // GET /api/users/{id} 查询单个用户
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable @Min(value = 1, message = "User Id must be at least 1") Integer id){
        User user =userService.getById(id);
        return Result.success(user);
    }

    // POST /api/users 新增用户
    @PostMapping
    public Result<User> create(@Valid @RequestBody UserCreateDTO dto){
        User user=new User();
        BeanUtils.copyProperties(dto, user);
        User createdUser = userService.create(user);
        return Result.success(createdUser);
    }

    // PUT /api/users 更新用户
    @PutMapping
    public Result<User> update(@Valid @RequestBody UserUpdateDTO dto){
        User user=new User();
        BeanUtils.copyProperties(dto, user);
        User updatedUser = userService.update(user);
        return Result.success(updatedUser);
    }

    // DELETE /api/users/{id} 删除用户
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable @Min(value = 1, message = "User Id must be at least 1") Integer id){
        userService.deleteById(id);
        return Result.success();
    }
}
