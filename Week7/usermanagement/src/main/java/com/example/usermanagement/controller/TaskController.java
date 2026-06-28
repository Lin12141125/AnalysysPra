package com.example.usermanagement.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.usermanagement.common.Result;
import com.example.usermanagement.dto.TaskCreateDTO;
import com.example.usermanagement.dto.TaskStatusUpdateDTO;
import com.example.usermanagement.dto.TaskUpdateDTO;
import com.example.usermanagement.exception.BusinessException;
import com.example.usermanagement.security.SecurityUserDetails;
import com.example.usermanagement.service.TaskService;
import com.example.usermanagement.vo.TaskVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

@RestController
@Validated
@Tag(name = "任务管理", description = "项目任务的创建、查询、更新和删除接口")
public class TaskController {
    @Autowired
    private TaskService taskService;

    @PostMapping("/api/projects/{projectId}/tasks")
    @Operation(summary = "创建任务", description = "在指定项目下创建一个新任务，只有项目的OWNER和MEMBER可以创建任务，VIEWER只能查看任务")
    public Result<TaskVO> createTask(
            @Parameter(description = "项目ID，必须为正整数", required = true, example = "1")
            @PathVariable @Min(1) Integer projectId,
            @Valid @RequestBody TaskCreateDTO dto) {
        Integer currentUserId = getCurrentUserId();
        TaskVO task = taskService.createTask(projectId, dto, currentUserId);
        return Result.success(task);
    }

    @GetMapping("/api/projects/{projectId}/tasks")
    @Operation(summary = "查询任务列表", description = "分页查询项目下任务，支持按状态、优先级、负责人筛选")
    public Result<Page<TaskVO>> listTasks(
            @Parameter(description = "项目ID，必须为正整数", required = true, example = "1")
            @PathVariable @Min(1) Integer projectId,
            @Parameter(description = "任务状态", example = "TODO")
            @RequestParam(required = false)
            @Pattern(regexp = "TODO|IN_PROGRESS|IN_REVIEW|DONE", message = "任务状态只能是TODO、IN_PROGRESS、IN_REVIEW或DONE")
            String status,
            @Parameter(description = "任务优先级", example = "HIGH")
            @RequestParam(required = false)
            @Pattern(regexp = "LOW|MEDIUM|HIGH|URGENT", message = "任务优先级只能是LOW、MEDIUM、HIGH或URGENT")
            String priority,
            @Parameter(description = "负责人用户ID", example = "2")
            @RequestParam(required = false) @Positive(message = "负责人ID必须为正整数") Integer assigneeId,
            @Parameter(description = "页码，必须为正整数，默认值为1", example = "1")
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @Parameter(description = "每页大小，范围1-100，默认值为10", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
        Integer currentUserId = getCurrentUserId();
        Page<TaskVO> tasks = taskService.listTasks(projectId, status, priority, assigneeId, page, size, currentUserId);
        return Result.success(tasks);
    }

    @PutMapping("/api/tasks/{id}")
    @Operation(summary = "更新任务", description = "更新任务信息，只有项目的OWNER和任务负责人可以更新任务，VIEWER只读")
    public Result<TaskVO> updateTask(
            @Parameter(description = "任务ID，必须为正整数", required = true, example = "1")
            @PathVariable @Min(1) Integer id,
            @Valid @RequestBody TaskUpdateDTO dto) {
        Integer currentUserId = getCurrentUserId();
        TaskVO task = taskService.updateTask(id, dto, currentUserId);
        return Result.success(task);
    }

    @DeleteMapping("/api/tasks/{id}")
    @Operation(summary = "删除任务", description = "删除任务，OWNER可以删除任意任务，MEMBER只能删除自己负责的任务，VIEWER只读")
    public Result<Void> deleteTask(
            @Parameter(description = "任务ID，必须为正整数", required = true, example = "1")
            @PathVariable @Min(1) Integer id) {
        Integer currentUserId = getCurrentUserId();
        taskService.deleteTask(id, currentUserId);
        return Result.success();
    }

    @PatchMapping("/api/tasks/{id}/status")
    @Operation(summary = "更新任务状态", description = "更新任务状态，按TODO->IN_PROGRESS->IN_REVIEW->DONE顺序流转任务状态，不能跳过中间状态只有项目的OWNER和任务负责人可以更新任务状态，VIEWER只读")
    public Result<TaskVO> updateTaskStatus(
            @Parameter(description = "任务ID，必须为正整数", required = true, example = "1")
            @PathVariable @Min(1) Integer id,
            @Valid @RequestBody TaskStatusUpdateDTO dto) {
        Integer currentUserId = getCurrentUserId();
        TaskVO task = taskService.updateTaskStatus(id, dto, currentUserId);
        return Result.success(task);
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUserDetails userDetails)) {
            throw new BusinessException(401, "用户未登录");
        }
        return userDetails.getUser().getId();
    }
}
