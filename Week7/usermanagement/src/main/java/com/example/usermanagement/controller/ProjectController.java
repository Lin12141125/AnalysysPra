package com.example.usermanagement.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.usermanagement.common.Result;
import com.example.usermanagement.dto.ProjectCreateDTO;
import com.example.usermanagement.dto.ProjectUpdateDTO;
import com.example.usermanagement.exception.BusinessException;
import com.example.usermanagement.security.SecurityUserDetails;
import com.example.usermanagement.service.ProjectService;
import com.example.usermanagement.vo.ProjectDetailVO;
import com.example.usermanagement.vo.ProjectListVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/projects")
@Validated
@Tag(name = "项目管理", description = "项目创建、查询、更新和删除接口")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @PostMapping
    @Operation(summary = "创建项目", description = "创建一个新的项目，创建者自动成为项目OWNER")
    public Result<ProjectDetailVO> createProject(@Valid @RequestBody ProjectCreateDTO dto){
        Integer currentUserId = getCurrentUserId();
        ProjectDetailVO projectDetail = projectService.createProject(dto, currentUserId);
        return Result.success(projectDetail);
    }

    @GetMapping
    @Operation(summary = "查询我参与的项目列表", description = "分页查询当前登录用户参与的项目列表")
    public Result<Page<ProjectListVO>> listMyProjects(
            @Parameter(description = "页码，必须为正整数，默认值为1", example = "1")
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @Parameter(description = "每页大小，范围1-100，默认值为10", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
        Integer currentUserId = getCurrentUserId();
        Page<ProjectListVO> projects = projectService.listMyProjects(currentUserId, page, size);
        return Result.success(projects);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查看项目详情", description = "根据项目ID查询项目的详细信息，包含成员列表和任务统计")
    public Result<ProjectDetailVO> getProjectDetail(
            @Parameter(description = "项目ID，必须为正整数", required = true, example = "1")
            @PathVariable @Min(1) Integer id) {
        Integer currentUserId = getCurrentUserId();
        ProjectDetailVO projectDetail = projectService.getProjectDetail(id, currentUserId);
        return Result.success(projectDetail);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新项目信息", description = "仅项目OWNER可以更新项目信息")
    public Result<ProjectDetailVO> updateProject(
            @Parameter(description = "项目ID，必须为正整数", required = true, example = "1")
            @PathVariable @Min(1) Integer id,
            @Valid @RequestBody ProjectUpdateDTO dto) {
        Integer currentUserId = getCurrentUserId();
        ProjectDetailVO updatedProject = projectService.updateProject(id, dto, currentUserId);
        return Result.success(updatedProject);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除项目", description = "仅项目OWNER可以删除项目，关联任务、评论、附件依赖数据库外键级联删除")
    public Result<Void> deleteProject(
            @Parameter(description = "项目ID，必须为正整数", required = true, example = "1")
            @PathVariable @Min(1) Integer id) {
        Integer currentUserId = getCurrentUserId();
        projectService.deleteProject(id, currentUserId);
        return Result.success();
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUserDetails userDetails)) {
            throw new BusinessException(401, "用户未登录");
        }
        return userDetails.getUser().getId();
    }
}
