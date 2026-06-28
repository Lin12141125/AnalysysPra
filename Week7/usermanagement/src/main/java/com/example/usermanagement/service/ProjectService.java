package com.example.usermanagement.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.usermanagement.dto.ProjectCreateDTO;
import com.example.usermanagement.dto.ProjectMemberInviteDTO;
import com.example.usermanagement.dto.ProjectUpdateDTO;
import com.example.usermanagement.vo.ProjectDetailVO;
import com.example.usermanagement.vo.ProjectListVO;
import com.example.usermanagement.vo.ProjectMemberVO;

public interface ProjectService {
    ProjectDetailVO createProject(ProjectCreateDTO dto, Integer currentUserId);
    Page<ProjectListVO> listMyProjects(Integer currentUserId, Integer page, Integer size);
    ProjectDetailVO getProjectDetail(Integer projectId, Integer currentUserId);
    ProjectDetailVO updateProject(Integer projectId, ProjectUpdateDTO dto, Integer currentUserId);
    void deleteProject(Integer projectId, Integer currentUserId);

    ProjectMemberVO inviteMember(Integer projectId, ProjectMemberInviteDTO dto, Integer currentUserId);
    void removeMember(Integer projectId, Integer targetUserId, Integer currentUserId);
    List<ProjectMemberVO> listMembers(Integer projectId, Integer currentUserId);
}
