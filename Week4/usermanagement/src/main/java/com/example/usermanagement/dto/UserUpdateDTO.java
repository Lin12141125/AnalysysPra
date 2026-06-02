package com.example.usermanagement.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/***
     * UserUpgrateDTO class: 更新用户信息 参数校验
 */

@Data
public class UserUpdateDTO {
    @NotNull(message = "User Id cannot be null")
    private Integer id;     // 可以是 "" 或 "   "，但不能为 null（可以空但不能Null）

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 1, max = 50, message = "Name must be between 2 and 50 characters")
    private String username;

    @Email(message = "Email must be valid")
    private String email;

    @Min(value = 1, message = "Age must be at least 1")
    @Max(value = 150, message = "Age must be less than 150")
    private Integer age;
}
