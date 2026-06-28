package com.example.usermanagement.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建任务请求参数")
public class TaskCreateDTO {
    @NotBlank(message = "任务标题不能为空")
    @Size(max = 150, message = "任务标题长度不能超过150个字符")
    @Schema(description = "任务标题", example = "task2")
    private String title;

    @Size(max = 1000, message = "任务描述长度不能超过1000个字符")
    @Schema(description = "任务描述", example = "这是一个任务描述")
    private String description;

    @Pattern(regexp="LOW|MEDIUM|HIGH|URGENT", message = "任务优先级只能是LOW、MEDIUM、HIGH或URGENT")
    @Schema(description = "任务优先级", example = "HIGH")
    private String priority= "MEDIUM"; // 未设置时默认为MEDIUM

    @Positive(message = "负责人ID必须为正整数")
    @Schema(description = "负责人ID(可选)，必须是项目成员", example = "1")
    private Integer assigneeId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "截止日期(可选)", example = "2026-07-10 18:00:00")
    private LocalDateTime deadline;
}
