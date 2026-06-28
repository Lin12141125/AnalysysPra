package com.example.usermanagement.service;

import java.util.List;

import com.example.usermanagement.dto.TaskCommentCreateDTO;
import com.example.usermanagement.vo.TaskCommentVO;

public interface TaskCommentService {
    TaskCommentVO addComment(Integer taskId, TaskCommentCreateDTO dto, Integer currentUserId);
    List<TaskCommentVO> listComments(Integer taskId, Integer currentUserId);
    void deleteComment(Integer commentId, Integer currentUserId);
}
