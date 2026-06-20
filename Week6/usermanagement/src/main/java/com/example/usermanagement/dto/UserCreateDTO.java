package com.example.usermanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

/***
 * UserCreateDTO class: 新增用户 参数校验
 */
@Data
@Schema(description = "新增用户请求参数")
public class UserCreateDTO {
    @NotBlank(message = "用户名不能为空")
    // 不能空不能Null 不能为 null、""、"   "
    @Size(min = 1, max = 50, message = "用户名长度必须在1到50之间")
    @Schema(description = "用户名", required = true, example = "user123")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", required = true, example = "user@example.com")
    private String email;

    @Min(value = 1, message = "年龄必须在1到150之间")
    @Max(value = 150, message = "年龄必须在1到150之间")
    @Schema(description = "年龄", required = true, example = "25")
    private  Integer age;
}
