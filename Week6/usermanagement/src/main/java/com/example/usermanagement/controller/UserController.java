package com.example.usermanagement.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.usermanagement.common.Result;
import com.example.usermanagement.dto.UserCreateDTO;
import com.example.usermanagement.dto.UserUpdateDTO;
import com.example.usermanagement.entity.User;
import com.example.usermanagement.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "用户管理", description = "用户信息的增删改查接口，权限控制基于角色") // Swagger API 文档分组和描述
public class UserController {

    @Autowired
    private UserService userService;

    // GET /api/users 查询所有用户
    @GetMapping
    @Operation(summary = "查询用户列表", description = "查询所有用户信息，返回所有用户列表")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')") // ADMIN 或 USER 角色可以查询
    public Result<List<User>> list() {
        List<User> users = userService.listAll();
        return Result.success(users);
    }

    // GET /api/users/{id} 查询单个用户
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询用户", description = "根据用户ID查询单个用户的详细信息，返回指定用户详情")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')") // ADMIN 或 USER 角色可以查询
    public Result<User> getById(
        @Parameter(description = "用户ID，必须为正整数", required = true, example = "1")
        @PathVariable @Min(value = 1, message = "User Id must be at least 1") Integer id){
        User user =userService.getById(id);
        return Result.success(user);
    }

    // POST /api/users 新增用户
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // 只有 ADMIN 角色可以新增
    @Operation(summary = "新增用户", description = "新增用户信息，仅限ADMIN角色操作")
    public Result<User> create(@Valid @RequestBody UserCreateDTO dto){
        User user=new User();
        BeanUtils.copyProperties(dto, user);
        User createdUser = userService.create(user);
        return Result.success(createdUser);
    }

    // PUT /api/users 更新用户
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // 只有 ADMIN 角色可以更新
    @Operation(summary = "更新用户", description = "更新用户信息，仅限ADMIN角色操作")
    public Result<User> update(
        @Parameter(description = "用户ID，必须为正整数", required = true, example = "1")
        @PathVariable @Min(1) Integer id, @Valid @RequestBody UserUpdateDTO dto){
        User updatedUser = userService.update(id, dto);
        return Result.success(updatedUser);
    }

    // DELETE /api/users/{id} 删除用户
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "根据用户ID删除用户，仅限ADMIN角色操作")
    @PreAuthorize("hasRole('ADMIN')") // 只有 ADMIN 角色可以删除
    public Result<String> delete(@Parameter(description = "用户ID，必须为正整数", required = true, example = "1")
                                 @PathVariable @Min(value = 1, message = "User Id must be at least 1") Integer id){
        userService.deleteById(id);
        return Result.success();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询用户", description = "分页查询用户列表，支持根据用户名模糊搜索，返回分页结果")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')") // ADMIN 或 USER 角色可以查询
    public Result<Page<User>> pageQuery(
        @Parameter(description = "页码，必须为正整数，默认值为1", example = "1")
        @RequestParam(defaultValue = "1") @Min(1) Integer page,
        @Parameter(description = "每页大小，范围1-100，默认值为10", example = "10")
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size, //无 @Min/@Max：传入 page=-1&size=10000 可能拖垮数据库
        @Parameter(description = "搜索关键字（用户名）", example = "zhang")
        @RequestParam(required = false) String keyword){
        Page<User> userPage=userService.pageQuery(page, size, keyword);
        return Result.success(userPage);
    }
}
