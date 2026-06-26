package com.example.usermanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "注册请求参数")
public class RegisterDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 1, max = 50, message = "用户名长度必须在1到50之间")
    @Schema(description = "用户名", required = true, example = "user123")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度至少6位")
    @Schema(description = "密码", required = true, example = "password123")
    private String password;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", required = true, example = "user@example.com")
    private String email;

    @Min(value = 1, message = "年龄必须在1到150之间")
    @Max(value = 150, message = "年龄必须在1到150之间")
    @Schema(description = "年龄", required = true, example = "25")
    private Integer age;
}
