package com.example.usermanagement.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "更新任务请求参数")
public class TaskUpdateDTO {
    @Size(max = 150, message = "任务标题长度不能超过150个字符")
    @Schema(description = "任务标题", example = "task1-updated")
    private String title;

    @Size(max = 1000, message = "任务描述长度不能超过1000个字符")
    @Schema(description = "任务描述", example = "task1 updated description")
    private String description;

    @Pattern(regexp = "LOW|MEDIUM|HIGH|URGENT", message = "任务优先级只能是LOW、MEDIUM、HIGH或URGENT")
    @Schema(description = "任务优先级", example = "HIGH")
    private String priority;

    @Positive(message = "负责人ID必须为正整数")
    @Schema(description = "负责人用户ID，必须是项目成员", example = "2")
    private Integer assigneeId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "截止时间", example = "2026-07-03 18:00:00")
    private LocalDateTime deadline;
}
