package com.example.usermanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "任务分配请求参数")
public class TaskAssignDTO {
    @NotNull(message = "任务负责人ID不能为空")
    @Positive(message = "任务负责人ID必须为正整数")
    @Schema(description = "任务负责人用户ID，必须是该任务所属项目的成员", example = "2")
    private Integer assigneeId;
}
