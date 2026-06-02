package com.example.usermanagement.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/***
 * UserCreateDTO class: 新增用户 参数校验
 */
@Data
public class UserCreateDTO {
    @NotBlank(message = "Name cannot be blank")
    // 不能空不能Null 不能为 null、""、"   "
    @Size(min = 1, max = 50, message = "Name must be between 2 and 50 characters")
    private String username;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be valid")
    private String email;

    @Min(value = 1, message = "Age must be at least 1")
    @Max(value = 150, message = "Age must be less than 150")
    private  Integer age;
}
