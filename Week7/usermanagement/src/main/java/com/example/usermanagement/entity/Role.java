package com.example.usermanagement.entity;

import lombok.Data;

@Data
public class Role {
    private Integer id;
    private String name; // ROLE_ADMIN, ROLE_USER
}
