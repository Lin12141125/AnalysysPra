package com.example.usermanagement.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.usermanagement.dto.ProjectCreateDTO;
import com.example.usermanagement.dto.ProjectUpdateDTO;
import com.example.usermanagement.vo.ProjectDetailVO;
import com.example.usermanagement.vo.ProjectListVO;

public interface ProjectService {
    ProjectDetailVO createProject(ProjectCreateDTO dto, Integer currentUserId);
    Page<ProjectListVO> listMyProjects(Integer currentUserId, Integer page, Integer size);
    ProjectDetailVO getProjectDetail(Integer projectId, Integer currentUserId);
    ProjectDetailVO updateProject(Integer projectId, ProjectUpdateDTO dto, Integer currentUserId);
    void deleteProject(Integer projectId, Integer currentUserId);
}
