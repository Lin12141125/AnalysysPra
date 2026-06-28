package com.example.usermanagement.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class TaskCommentVO {
    private Integer id;
    private Integer taskId;
    private Integer userId;
    private String username;
    private String content;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String createdAt;
}
