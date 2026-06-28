package com.example.usermanagement.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class TaskAttachmentVO {
    private Integer id;
    private Integer taskId;
    private String filename;
    private String originalName;
    private Long fileSize;
    private Integer uploadedBy;
    private String uploadedByUsername;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
