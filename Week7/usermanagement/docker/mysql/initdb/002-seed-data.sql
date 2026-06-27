USE `user_db`;

INSERT INTO `role` (`name`) VALUES
('ROLE_ADMIN'),
('ROLE_USER');

INSERT INTO `user` (`username`, `email`, `password`, `age`, `avatar`, `created_at`) VALUES
('admin', 'admin@example.com', '$2a$10$cH/hYF6UhBKjPU2Ds7.xIO6YFU4NzMT5NXpPwUptTSC80xvcwZ7Z.', 30, NULL, NOW()),
('user', 'user@example.com', '$2a$10$cH/hYF6UhBKjPU2Ds7.xIO6YFU4NzMT5NXpPwUptTSC80xvcwZ7Z.', 25, NULL, NOW()),
('member', 'member@example.com', '$2a$10$cH/hYF6UhBKjPU2Ds7.xIO6YFU4NzMT5NXpPwUptTSC80xvcwZ7Z.', 24, NULL, NOW()),
('viewer', 'viewer@example.com', '$2a$10$cH/hYF6UhBKjPU2Ds7.xIO6YFU4NzMT5NXpPwUptTSC80xvcwZ7Z.', 22, NULL, NOW());

INSERT INTO `user_role` (`user_id`, `role_id`) VALUES
(1, 1),
(2, 2),
(3, 2),
(4, 2);

INSERT INTO `project` (`name`, `description`, `owner_id`, `created_at`, `updated_at`) VALUES
('project1', 'project1 description', 1, NOW(), NOW()),
('project2', 'project2 description', 2, NOW(), NOW());

INSERT INTO `project_member` (`project_id`, `user_id`, `role`) VALUES
(1, 1, 'OWNER'),
(1, 2, 'MEMBER'),
(1, 3, 'MEMBER'),
(1, 4, 'VIEWER'),
(2, 2, 'OWNER'),
(2, 1, 'MEMBER'),
(2, 4, 'VIEWER');

INSERT INTO `task` (`project_id`, `title`, `description`, `status`, `priority`, `assignee_id`, `deadline`, `created_at`, `updated_at`) VALUES
(1, 'task1', 'task1 description', 'TODO', 'LOW', 1, DATE_ADD(NOW(), INTERVAL 1 DAY), NOW(), NOW()),
(1, 'task2', 'task2 description', 'IN_PROGRESS', 'MEDIUM', 2, DATE_ADD(NOW(), INTERVAL 2 DAY), NOW(), NOW()),
(1, 'task3', 'task3 description', 'IN_REVIEW', 'HIGH', 3, DATE_ADD(NOW(), INTERVAL 3 DAY), NOW(), NOW()),
(1, 'task4', 'task4 description', 'DONE', 'URGENT', NULL, DATE_ADD(NOW(), INTERVAL 4 DAY), NOW(), NOW()),
(2, 'task5', 'task5 description', 'TODO', 'MEDIUM', 1, DATE_ADD(NOW(), INTERVAL 5 DAY), NOW(), NOW());

INSERT INTO `task_comment` (`task_id`, `user_id`, `content`, `created_at`) VALUES
(1, 1, 'task1''s comment1', NOW()),
(1, 2, 'task1''s comment2', NOW()),
(2, 2, 'task2''s comment1', NOW()),
(3, 3, 'task3''s comment1', NOW());

-- INSERT INTO `task_attachment` (`task_id`, `filename`, `original_name`, `file_size`, `uploaded_by`, `created_at`) VALUES
-- (1, 'task1-attachment1.txt', 'task1-attachment1.txt', 1024, 1, NOW()),
-- (2, 'task2-attachment1.txt', 'task2-attachment1.txt', 2048, 2, NOW());