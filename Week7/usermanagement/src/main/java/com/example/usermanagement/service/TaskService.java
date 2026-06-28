package com.example.usermanagement.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.usermanagement.dto.TaskCreateDTO;
import com.example.usermanagement.dto.TaskStatusUpdateDTO;
import com.example.usermanagement.dto.TaskUpdateDTO;
import com.example.usermanagement.vo.TaskVO;

public interface TaskService {
    TaskVO createTask(Integer projectId, TaskCreateDTO dto, Integer currentUserId);
    Page<TaskVO> listTasks(
            Integer projectId,
            String status,
            String priority,
            Integer assigneeId,
            Integer page,
            Integer size,
            Integer currentUserId
    );
    TaskVO updateTask(Integer taskId, TaskUpdateDTO dto, Integer currentUserId);
    void deleteTask(Integer taskId, Integer currentUserId);

    TaskVO updateTaskStatus(Integer taskId, TaskStatusUpdateDTO dto, Integer currentUserId);
}
