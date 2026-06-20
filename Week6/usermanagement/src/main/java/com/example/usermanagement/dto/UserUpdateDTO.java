package com.example.usermanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

/***
     * UserUpdateDTO class: 更新用户信息 参数校验
 */

@Data
@Schema(description = "更新用户信息请求参数")
public class UserUpdateDTO {
    // PUT接口改为路径参数：将 @PutMapping 改为 @PutMapping("/{id}")，并从路径获取id
    // --> 同时DTO中不再需要id字段，只传需要更新的字段
    
    // @NotNull(message = "User Id cannot be null")
    // private Integer id;     // 可以是 "" 或 "   "，但不能为 null（可以空但不能Null）

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 1, max = 50, message = "Name must be between 1 and 50 characters")
    @Schema(description = "用户名", required = true, example = "user123")
    private String username;

    @Email(message = "Email must be valid")
    @Schema(description = "邮箱", example = "user@example.com")
    private String email;

    @Min(value = 1, message = "Age must be at least 1")
    @Max(value = 150, message = "Age must be less than 150")
    @Schema(description = "年龄", example = "25")
    private Integer age;
}
