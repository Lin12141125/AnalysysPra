package com.example.usermanagement.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
@TableName("task_attachment")
public class TaskAttachment {
    private Integer id;
    private Integer taskId;
    private String filename;
    private String originalName;
    private Long fileSize;
    private Integer uploadedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
