package com.example.usermanagement.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.usermanagement.dto.TaskCommentCreateDTO;
import com.example.usermanagement.entity.ProjectMember;
import com.example.usermanagement.entity.Task;
import com.example.usermanagement.entity.TaskComment;
import com.example.usermanagement.exception.BusinessException;
import com.example.usermanagement.mapper.ProjectMemberMapper;
import com.example.usermanagement.mapper.TaskCommentMapper;
import com.example.usermanagement.mapper.TaskMapper;
import com.example.usermanagement.service.TaskCommentService;
import com.example.usermanagement.vo.TaskCommentVO;

@Service
public class TaskCommentServiceImpl implements TaskCommentService {
    private static final String ROLE_OWNER = "OWNER";
    private static final String ROLE_VIEWER = "VIEWER";

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private TaskCommentMapper taskCommentMapper;

    @Autowired
    private ProjectMemberMapper projectMemberMapper;

    @Override
    @Transactional
    public TaskCommentVO addComment(Integer taskId, TaskCommentCreateDTO dto, Integer currentUserId) {
        Task task=getTaskOrThrow(taskId); // 验证task存在性
        ProjectMember currentMember=getProjectMemberOrThrow(task.getProjectId(), currentUserId); // 验证当前用户是项目成员

        // 当前角色是viewer-->不能添加评论
        if(ROLE_VIEWER.equals(currentMember.getRole())) {
            throw new BusinessException(403, "VIEWER只能查看评论，不能添加评论");
        }

        TaskComment comment = new TaskComment();
        comment.setTaskId(taskId);
        comment.setUserId(currentUserId);
        comment.setContent(dto.getContent());
        comment.setCreatedAt(LocalDateTime.now());
        taskCommentMapper.insert(comment);

        return getCommentVOOrThrow(comment.getId());
    }

    @Override
    public List<TaskCommentVO> listComments(Integer taskId, Integer currentUserId) {
        Task task=getTaskOrThrow(taskId); // 验证task存在性
        getProjectMemberOrThrow(task.getProjectId(), currentUserId); // 验证当前用户是项目成员

        return taskCommentMapper.selectCommentsByTaskId(taskId);
    }

    @Override
    @Transactional
    public void deleteComment(Integer commentId, Integer currentUserId) {
        TaskComment comment=getCommentOrThrow(commentId); // 验证评论存在性
        Task task=getTaskOrThrow(comment.getTaskId()); // 验证task存在性
        ProjectMember currentMember=getProjectMemberOrThrow(task.getProjectId(), currentUserId);

        // 如果是OWNER-->可以删除任何评论
        if(ROLE_OWNER.equals(currentMember.getRole())){
            taskCommentMapper.deleteById(commentId);
            return;
        } // OWNER直接删除评论，删完退出逻辑判断

        // 如果是VIEWER-->不能删除评论
        if(ROLE_VIEWER.equals(currentMember.getRole())){
            throw new BusinessException(403, "VIEWER只能查看评论，不能删除评论");
        }

        // MEMBER
        // 非自己的评论-->无法删除
        if(!comment.getUserId().equals(currentUserId)){
            throw new BusinessException(403, "MEMBER只能删除自己发布的评论");
        }
        // 自己的评论-->可以删除
        taskCommentMapper.deleteById(commentId);
    }

    private Task getTaskOrThrow(Integer taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在，id=" + taskId);
        }
        return task;
    }

    private ProjectMember getProjectMemberOrThrow(Integer projectId, Integer userId) {
        ProjectMember member = getProjectMember(projectId, userId);
        if (member == null) {
            throw new BusinessException(403, "用户不是项目成员，无权访问任务评论");
        }
        return member;
    }

    private ProjectMember getProjectMember(Integer projectId, Integer userId) {
        LambdaQueryWrapper<ProjectMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId);
        return projectMemberMapper.selectOne(queryWrapper);
    }

    private TaskCommentVO getCommentVOOrThrow(Integer commentId) {
        TaskCommentVO commentVO = taskCommentMapper.selectCommentVOById(commentId);
        if (commentVO == null) {
            throw new BusinessException(404, "评论不存在，id=" + commentId);
        }
        return commentVO;
    }

    private TaskComment getCommentOrThrow(Integer commentId) {
        TaskComment comment = taskCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(404, "评论不存在，id=" + commentId);
        }
        return comment;
    }
}
