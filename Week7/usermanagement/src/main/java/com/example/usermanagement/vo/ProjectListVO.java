package com.example.usermanagement.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class ProjectListVO {
    private Integer id;
    private String name;
    private String description;
    private Integer ownerId;
    private String currentUserRole; // 当前用户在该项目中的角色，可能的值为 OWNER, MEMBER, VIEWER

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
