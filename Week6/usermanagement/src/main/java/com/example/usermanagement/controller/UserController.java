package com.example.usermanagement.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.usermanagement.common.Result;
import com.example.usermanagement.dto.UserCreateDTO;
import com.example.usermanagement.dto.UserUpdateDTO;
import com.example.usermanagement.entity.User;
import com.example.usermanagement.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')") // ADMIN 或 USER 角色可以查询
    public Result<List<User>> list() {
        List<User> users = userService.listAll();
        return Result.success(users);
    }

    // GET /api/users/{id} 查询单个用户
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')") // ADMIN 或 USER 角色可以查询
    public Result<User> getById(@PathVariable @Min(value = 1, message = "User Id must be at least 1") Integer id){
        User user =userService.getById(id);
        return Result.success(user);
    }

    // POST /api/users 新增用户
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // 只有 ADMIN 角色可以新增
    public Result<User> create(@Valid @RequestBody UserCreateDTO dto){
        User user=new User();
        BeanUtils.copyProperties(dto, user);
        User createdUser = userService.create(user);
        return Result.success(createdUser);
    }

    // PUT /api/users 更新用户
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // 只有 ADMIN 角色可以更新
    public Result<User> update(@PathVariable @Min(1) Integer id, @Valid @RequestBody UserUpdateDTO dto){
        User updatedUser = userService.update(id, dto);
        return Result.success(updatedUser);
    }

    // DELETE /api/users/{id} 删除用户
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // 只有 ADMIN 角色可以删除
    public Result<String> delete(@PathVariable @Min(value = 1, message = "User Id must be at least 1") Integer id){
        userService.deleteById(id);
        return Result.success();
    }

    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')") // ADMIN 或 USER 角色可以查询
    public Result<Page<User>> pageQuery(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size, //无 @Min/@Max：传入 page=-1&size=10000 可能拖垮数据库
            @RequestParam(required = false) String keyword){
        Page<User> userPage=userService.pageQuery(page, size, keyword);
        return Result.success(userPage);
    }
}
