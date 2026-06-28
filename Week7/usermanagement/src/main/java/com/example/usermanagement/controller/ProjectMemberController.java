package com.example.usermanagement.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.example.usermanagement.common.Result;
import com.example.usermanagement.dto.ProjectMemberInviteDTO;
import com.example.usermanagement.exception.BusinessException;
import com.example.usermanagement.security.SecurityUserDetails;
import com.example.usermanagement.service.ProjectService;
import com.example.usermanagement.vo.ProjectMemberVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/projects/{projectId}/members")
@Validated
@Tag(name = "项目成员管理", description = "项目成员邀请、移除和查询接口")
public class ProjectMemberController {
    @Autowired
    private ProjectService projectService;

    @PostMapping
    @Operation(summary = "邀请项目成员", description = "仅项目OWNER可以邀请成员，只能分配MEMBER或VIEWER角色")
    public Result<ProjectMemberVO> inviteMember(
            @Parameter(description = "项目ID，必须为正整数", required = true, example = "1")
            @PathVariable @Min(1) Integer projectId,
            @Valid @RequestBody ProjectMemberInviteDTO dto) {
        Integer currentUserId = getCurrentUserId();
        ProjectMemberVO member = projectService.inviteMember(projectId, dto, currentUserId);
        return Result.success(member);
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "移除项目成员", description = "仅项目OWNER可以移除成员，不能移除项目OWNER")
    public Result<Void> removeMember(
            @Parameter(description = "项目ID，必须为正整数", required = true, example = "1")
            @PathVariable @Min(1) Integer projectId,
            @Parameter(description = "被移除用户ID，必须为正整数", required = true, example = "1")
            @PathVariable @Min(1) Integer userId) {
        Integer currentUserId = getCurrentUserId();
        projectService.removeMember(projectId, userId, currentUserId);
        return Result.success();
    }

    @GetMapping
    @Operation(summary = "查询项目成员列表", description = "项目OWNER和MEMBER可以查看项目成员列表")
    public Result<List<ProjectMemberVO>> listMembers(
            @Parameter(description = "项目ID，必须为正整数", required = true, example = "1")
            @PathVariable @Min(1) Integer projectId) {
        Integer currentUserId = getCurrentUserId();
        List<ProjectMemberVO> members = projectService.listMembers(projectId, currentUserId);
        return Result.success(members);
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUserDetails userDetails)) {
            throw new BusinessException(401, "用户未登录");
        }
        return userDetails.getUser().getId();
    }
}
