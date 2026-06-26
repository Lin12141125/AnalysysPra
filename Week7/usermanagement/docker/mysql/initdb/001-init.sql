DROP DATABASE IF EXISTS `user_db`;
CREATE DATABASE `user_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `user_db`;

CREATE TABLE `user` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `email` VARCHAR(100) NOT NULL COMMENT '邮箱',
    `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt加密后的密码',
    `age` INT COMMENT '年龄',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像文件名（UUID.扩展名）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE `role` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL COMMENT '角色名称，ROLE_ADMIN / ROLE_USER',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE `user_role` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `user_id` INT NOT NULL COMMENT '用户ID',
    `role_id` INT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

INSERT INTO `role` (`name`) VALUES
('ROLE_ADMIN'),
('ROLE_USER');

INSERT INTO `user` (`username`, `email`, `password`, `age`, `avatar`, `created_at`) VALUES
('admin', 'admin@example.com', '$2a$10$cH/hYF6UhBKjPU2Ds7.xIO6YFU4NzMT5NXpPwUptTSC80xvcwZ7Z.', 30, NULL, NOW()),
('user', 'user@example.com', '$2a$10$cH/hYF6UhBKjPU2Ds7.xIO6YFU4NzMT5NXpPwUptTSC80xvcwZ7Z.', 25, NULL, NOW());

INSERT INTO `user_role` (`user_id`, `role_id`) VALUES (1, 1);
INSERT INTO `user_role` (`user_id`, `role_id`) VALUES (2, 2);
