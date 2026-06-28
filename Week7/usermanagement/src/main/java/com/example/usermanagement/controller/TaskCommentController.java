package com.example.usermanagement.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.example.usermanagement.common.Result;
import com.example.usermanagement.dto.TaskCommentCreateDTO;
import com.example.usermanagement.exception.BusinessException;
import com.example.usermanagement.security.SecurityUserDetails;
import com.example.usermanagement.service.TaskCommentService;
import com.example.usermanagement.vo.TaskCommentVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@RestController
@Validated
@Tag(name = "任务评论管理", description = "任务评论的创建、查询和删除接口")
public class TaskCommentController {
    @Autowired
    private TaskCommentService taskCommentService;

    @PostMapping("/api/tasks/{taskId}/comments")
    @Operation(summary = "添加任务评论", description = "在指定任务下添加评论，只有项目的OWNER和MEMBER可以添加评论，VIEWER只能查看评论")
    public Result<TaskCommentVO> addComment(
            @Parameter(description = "任务ID，必须为正整数", required = true, example = "1")
            @PathVariable @Min(1) Integer taskId,
            @Valid @RequestBody TaskCommentCreateDTO dto) {
        Integer currentUserId = getCurrentUserId();
        TaskCommentVO comment = taskCommentService.addComment(taskId, dto, currentUserId);
        return Result.success(comment);
    }

    @GetMapping("/api/tasks/{taskId}/comments")
    @Operation(summary = "查询任务评论列表", description = "项目成员可以查看评论列表，按创建时间正序返回")
    public Result<List<TaskCommentVO>> listComments(
            @Parameter(description = "任务ID，必须为正整数", required = true, example = "1")
            @PathVariable @Min(1) Integer taskId) {
        Integer currentUserId = getCurrentUserId();
        List<TaskCommentVO> comments = taskCommentService.listComments(taskId, currentUserId);
        return Result.success(comments);
    }

    @DeleteMapping("/api/comments/{id}")
    @Operation(summary = "删除任务评论", description = "OWNER可以删除任意评论，MEMBER只能删除自己的评论，VIEWER只读")
    public Result<Void> deleteComment(
            @Parameter(description = "评论ID，必须为正整数", required = true, example = "1")
            @PathVariable @Min(1) Integer id) {
        Integer currentUserId = getCurrentUserId();
        taskCommentService.deleteComment(id, currentUserId);
        return Result.success();
    }

    private Integer getCurrentUserId(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUserDetails userDetails)) {
            throw new BusinessException(401, "用户未登录");
        }
        return userDetails.getUser().getId();
    }
}
