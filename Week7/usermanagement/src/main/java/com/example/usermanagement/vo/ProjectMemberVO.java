package com.example.usermanagement.vo;

import lombok.Data;

@Data
public class ProjectMemberVO {
    private Integer userId;
    private String username;
    private String email;
    private String role;
}