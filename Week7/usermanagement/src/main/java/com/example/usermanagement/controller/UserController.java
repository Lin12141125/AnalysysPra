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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

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

    // 上传头像（仅ADMIN）
    @PostMapping("/{id}/avatar")
    @Operation(summary = "上传用户头像", description = "上传用户头像，仅限ADMIN角色操作，支持 jpg/png，最大2MB")
    @PreAuthorize("hasRole('ADMIN')") // 只有 ADMIN 角色可以上传头像
    public Result<String> uploadAvatar(
        @Parameter(description = "用户ID，必须为正整数", required = true, example = "1")
        @PathVariable @Min(1) Integer id,
        @Parameter(description = "头像文件", required = true)
        @RequestParam("file") MultipartFile file) throws IOException{
        // 检查文件是否为空
        if (file.isEmpty()) {
            return Result.error(400, "文件不能为空");
        }

        // 检查文件类型（只允许jpg/png）
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equals("image/jpeg") || contentType.equals("image/png"))) {
            return Result.error(400, "仅支持 JPG 或 PNG 格式的图片");
        }

        // 获取原始文件名，提取扩展名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        // 若扩展名为空或不是.jpg/.png-->从contentType推断扩展名
        if (!extension.equalsIgnoreCase(".jpg") && !extension.equalsIgnoreCase(".jpeg") && !extension.equalsIgnoreCase(".png")){
            // 根据contentType补充扩展名
            if (contentType.equals("image/jpeg")) {
                extension = ".jpg";
            } else if (contentType.equals("image/png")) {
                extension = ".png";
            } else {
                return Result.error(400, "不支持的文件类型");
            }
        }

        // 生成UUID文件名，避免冲突
        String avatarFileName = UUID.randomUUID().toString() + extension;
        // 保存文件到./uploads目录（相对于项目根目录）
        String uploadDir = System.getProperty("user.dir") + File.separator + "uploads";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            boolean mkdirs= dir.mkdirs();
            if (!mkdirs) {
                throw new IOException("无法创建上传目录");
            }
        }
        File dest = new File(dir, avatarFileName);
        file.transferTo(dest);
        // 更新DB中的avatar字段
        userService.updateAvatar(id, avatarFileName);
        return Result.success("头像上传成功，文件名：" + avatarFileName);
    }

    // 获取用户头像（返回图片流）
    @GetMapping("/{id}/avatar")
    @Operation(summary = "获取用户头像", description = "返回图片二进制流，若无头像则返回404")
    public ResponseEntity<byte[]> getAvatar(
        @Parameter(description = "用户ID，必须为正整数", required = true, example = "1")
        @PathVariable @Min(1) Integer id) throws IOException {
        
        // 查询用户
        User user = userService.getById(id);
        if (user == null) return ResponseEntity.notFound().build();
        String avatarFileName = user.getAvatar();
        if(avatarFileName == null || avatarFileName.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // 构造文件路径
        String uploadDir = System.getProperty("user.dir") + File.separator + "uploads";
        Path filePath = Paths.get(uploadDir, avatarFileName);
        File file = filePath.toFile();
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        // 读取字节文件
        byte[] fileContent = Files.readAllBytes(filePath);
        // 确定Content-Type
        String contentType = "application/octet-stream";
        if (avatarFileName.toLowerCase().endsWith(".jpg") || avatarFileName.toLowerCase().endsWith(".jpeg")) {
            contentType = "image/jpeg";
        } else if (avatarFileName.toLowerCase().endsWith(".png")) {
            contentType = "image/png";
        }
        // 返回响应
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + avatarFileName + "\"")
            .body(fileContent);
    }
}
