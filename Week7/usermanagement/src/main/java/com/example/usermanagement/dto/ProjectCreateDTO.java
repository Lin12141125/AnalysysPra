package com.example.usermanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建项目请求参数")
public class ProjectCreateDTO {
    @NotBlank(message = "项目名称不能为空")
    @Size(max = 100, message = "项目名称长度不能超过100个字符")
    @Schema(description = "项目名称", example = "新项目1")
    private String name;

    @Size(max = 500, message = "项目描述长度不能超过500个字符")
    @Schema(description = "项目描述", example = "这是一个新创建的项目")
    private String description;

}
