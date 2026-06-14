package com.example.usermanagement.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class User {
    private Integer id;
    private String username;
    private String email;
    private String password;
    private Integer age;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    // @JsonIgnore // 序列化和反序列化过程中某个字段应该被忽略时使用
    @TableField(exist = false) // 告诉MP该字段不是数据库字段
    private List<Role> roles; // 接收角色列表（非数据库字段）
}
