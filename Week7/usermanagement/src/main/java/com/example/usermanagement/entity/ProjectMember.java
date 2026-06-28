package com.example.usermanagement.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("project_member")
public class ProjectMember {
    private Integer id;
    private Integer projectId;
    private Integer userId;
    private String role; // OWNER, MEMBER, VIEWER
}
