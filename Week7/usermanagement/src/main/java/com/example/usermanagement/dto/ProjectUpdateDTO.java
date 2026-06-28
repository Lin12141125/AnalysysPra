package com.example.usermanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "更新项目请求参数")
public class ProjectUpdateDTO {
    @Size(max = 100, message = "项目名称长度不能超过100个字符")
    @Schema(description = "项目名称", example = "更新后的项目名称")
    private String name;

    @Size(max = 500, message = "项目描述长度不能超过500个字符")
    @Schema(description = "项目描述", example = "更新后的项目描述")
    private String description;
}
