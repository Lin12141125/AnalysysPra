package com.example.usermanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "任务状态流转请求参数")
public class TaskStatusUpdateDTO {
    @NotNull(message = "目标任务状态不能为空")
    @Pattern(regexp = "TODO|IN_PROGRESS|IN_REVIEW|DONE", message = "目标任务状态只能是TODO、IN_PROGRESS、IN_REVIEW或DONE")
    @Schema(description = "目标任务状态", example = "IN_PROGRESS",allowableValues = {"TODO", "IN_PROGRESS", "IN_REVIEW", "DONE"})
    private String status;
}
