DROP DATABASE IF EXISTS `user_db`;
CREATE DATABASE `user_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `user_db`;

CREATE TABLE `user` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `email` VARCHAR(100) NOT NULL COMMENT '邮箱',
    `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt加密后的密码',
    `age` INT DEFAULT NULL COMMENT '年龄',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像文件名（UUID.扩展名）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE `role` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `name` VARCHAR(50) NOT NULL COMMENT '系统角色名称，ROLE_ADMIN / ROLE_USER',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

CREATE TABLE `user_role` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '用户角色关联ID',
    `user_id` INT NOT NULL COMMENT '用户ID',
    `role_id` INT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`),
    CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户系统角色关联表';

CREATE TABLE `project` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '项目ID',
    `name` VARCHAR(100) NOT NULL COMMENT '项目名称',
    `description` TEXT DEFAULT NULL COMMENT '项目描述',
    `owner_id` INT NOT NULL COMMENT '项目创建者/负责人用户ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_owner_id` (`owner_id`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_project_owner` FOREIGN KEY (`owner_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目表';

CREATE TABLE `project_member` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '项目成员ID',
    `project_id` INT NOT NULL COMMENT '项目ID',
    `user_id` INT NOT NULL COMMENT '用户ID',
    `role` VARCHAR(20) NOT NULL COMMENT '项目角色：OWNER/MEMBER/VIEWER',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_project_user` (`project_id`, `user_id`),
    KEY `idx_user_role` (`user_id`, `role`),
    KEY `idx_project_role` (`project_id`, `role`),
    CONSTRAINT `fk_project_member_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_project_member_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `ck_project_member_role` CHECK (`role` IN ('OWNER', 'MEMBER', 'VIEWER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目成员表';

CREATE TABLE `task` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `project_id` INT NOT NULL COMMENT '所属项目ID',
    `title` VARCHAR(150) NOT NULL COMMENT '任务标题',
    `description` TEXT DEFAULT NULL COMMENT '任务描述',
    `status` VARCHAR(20) NOT NULL DEFAULT 'TODO' COMMENT '任务状态：TODO/IN_PROGRESS/IN_REVIEW/DONE',
    `priority` VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' COMMENT '优先级：LOW/MEDIUM/HIGH/URGENT',
    `assignee_id` INT DEFAULT NULL COMMENT '负责人用户ID',
    `deadline` DATETIME DEFAULT NULL COMMENT '截止时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_project_status_priority` (`project_id`, `status`, `priority`),
    KEY `idx_project_assignee` (`project_id`, `assignee_id`),
    KEY `idx_assignee_status` (`assignee_id`, `status`),
    KEY `idx_deadline` (`deadline`),
    CONSTRAINT `fk_task_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_task_assignee` FOREIGN KEY (`assignee_id`) REFERENCES `user` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_task_status` CHECK (`status` IN ('TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE')),
    CONSTRAINT `ck_task_priority` CHECK (`priority` IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表';

CREATE TABLE `task_comment` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '任务评论ID',
    `task_id` INT NOT NULL COMMENT '任务ID',
    `user_id` INT DEFAULT NULL COMMENT '评论人用户ID',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    PRIMARY KEY (`id`),
    KEY `idx_task_created_at` (`task_id`, `created_at`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_task_comment_task` FOREIGN KEY (`task_id`) REFERENCES `task` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_task_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务评论表';

CREATE TABLE `task_attachment` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '任务附件ID',
    `task_id` INT NOT NULL COMMENT '任务ID',
    `filename` VARCHAR(255) NOT NULL COMMENT '服务端存储文件名',
    `original_name` VARCHAR(255) NOT NULL COMMENT '用户上传时的原始文件名',
    `file_size` BIGINT NOT NULL COMMENT '文件大小，单位字节',
    `uploaded_by` INT DEFAULT NULL COMMENT '上传人用户ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (`id`),
    KEY `idx_task_created_at` (`task_id`, `created_at`),
    KEY `idx_uploaded_by` (`uploaded_by`),
    CONSTRAINT `fk_task_attachment_task` FOREIGN KEY (`task_id`) REFERENCES `task` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_task_attachment_user` FOREIGN KEY (`uploaded_by`) REFERENCES `user` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_task_attachment_file_size` CHECK (`file_size` > 0 AND `file_size` <= 10485760)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务附件表';