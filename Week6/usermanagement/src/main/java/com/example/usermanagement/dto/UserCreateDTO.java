package com.example.usermanagement.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/***
 * UserCreateDTO class: 新增用户 参数校验
 */
@Data
public class UserCreateDTO {
    @NotBlank(message = "用户名不能为空")
    // 不能空不能Null 不能为 null、""、"   "
    @Size(min = 1, max = 50, message = "用户名长度必须在1到50之间")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Min(value = 1, message = "年龄必须在1到150之间")
    @Max(value = 150, message = "年龄必须在1到150之间")
    private  Integer age;
}
