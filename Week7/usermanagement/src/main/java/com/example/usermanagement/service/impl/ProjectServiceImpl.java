package com.example.usermanagement.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.usermanagement.cache.ProjectCacheManager;
import com.example.usermanagement.dto.ProjectCreateDTO;
import com.example.usermanagement.dto.ProjectUpdateDTO;
import com.example.usermanagement.entity.Project;
import com.example.usermanagement.entity.ProjectMember;
import com.example.usermanagement.exception.BusinessException;
import com.example.usermanagement.mapper.ProjectMapper;
import com.example.usermanagement.mapper.ProjectMemberMapper;
import com.example.usermanagement.service.ProjectService;
import com.example.usermanagement.vo.ProjectDetailVO;
import com.example.usermanagement.vo.ProjectListVO;
import com.example.usermanagement.vo.ProjectMemberVO;
import com.example.usermanagement.vo.ProjectTaskStatsVO;

@Service
public class ProjectServiceImpl implements ProjectService {
    private static final String ROLE_OWNER = "OWNER";

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ProjectMemberMapper projectMemberMapper;

    @Autowired
    private ProjectCacheManager projectCacheManager;

    @Override
    @Transactional
    public ProjectDetailVO createProject(ProjectCreateDTO dto, Integer currentUserId) {
        LocalDateTime now = LocalDateTime.now();

        Project project = new Project();
        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setOwnerId(currentUserId);
        project.setCreatedAt(now);
        project.setUpdatedAt(now);
        projectMapper.insert(project);

        ProjectMember ownerMember = new ProjectMember();
        ownerMember.setProjectId(project.getId());
        ownerMember.setUserId(currentUserId);
        ownerMember.setRole(ROLE_OWNER);
        projectMemberMapper.insert(ownerMember);

        projectCacheManager.evictMyProjects(currentUserId);

        return getProjectDetail(project.getId(), currentUserId);
    }

    @Override
    public Page<ProjectListVO> listMyProjects(Integer currentUserId, Integer page, Integer size) {
        return projectCacheManager.queryMyProjects(currentUserId, page, size, () -> projectMapper.selectMyProjectsPage(new Page<>(page, size), currentUserId));
    }

    @Override
    public ProjectDetailVO getProjectDetail(Integer projectId, Integer currentUserId){
        return projectCacheManager.queryProjectDetail(projectId, currentUserId, () -> loadProjectDetailFromDb(projectId, currentUserId));
    }

    @Override
    @Transactional
    public ProjectDetailVO updateProject(Integer projectId, ProjectUpdateDTO dto, Integer currentUserId) {
        Project project = getProjectOrThrow(projectId);
        checkOwner(projectId, currentUserId);

        if(dto.getName()!=null) project.setName(dto.getName());
        if(dto.getDescription()!=null) project.setDescription(dto.getDescription());
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);
        projectCacheManager.evictProject(projectId);

        List<ProjectMemberVO> members = projectMemberMapper.selectMembersByProjectId(projectId);
        for (ProjectMemberVO member : members) {
            projectCacheManager.evictMyProjects(member.getUserId());
        }

        return getProjectDetail(projectId, currentUserId);
    }

    @Override
    @Transactional
    public void deleteProject(Integer projectId, Integer currentUserId) {
        getProjectOrThrow(projectId);
        checkOwner(projectId, currentUserId);

        List<ProjectMemberVO> members = projectMemberMapper.selectMembersByProjectId(projectId);
        projectMapper.deleteById(projectId);

        projectCacheManager.evictProject(projectId);
        for (ProjectMemberVO member : members) {
            projectCacheManager.evictMyProjects(member.getUserId());
        }
    }

    private ProjectDetailVO loadProjectDetailFromDb(Integer projectId, Integer currentUserId) {
        Project project = getProjectOrThrow(projectId);
        ProjectMember currentMember=getProjectMemberOrThrow(projectId, currentUserId);
        return buildProjectDetail(project, currentMember.getRole());
    }

    private Project getProjectOrThrow(Integer projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(404, "项目不存在，id=" + projectId);
        }
        return project;
    }

    private ProjectMember getProjectMemberOrThrow(Integer projectId, Integer userId) {
        ProjectMember member = getProjectMember(projectId, userId);
        if (member == null) {
            throw new BusinessException(403, "你不是该项目成员，无权访问该项目");
        }
        return member;
    }

    private ProjectMember getProjectMember(Integer projectId, Integer userId) {
        LambdaQueryWrapper<ProjectMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProjectMember::getProjectId, projectId)
                    .eq(ProjectMember::getUserId, userId);
        return projectMemberMapper.selectOne(queryWrapper);
    }

    private ProjectDetailVO buildProjectDetail(Project project, String currentUserRole) {
        ProjectDetailVO detailVO = new ProjectDetailVO();
        BeanUtils.copyProperties(project, detailVO);
        detailVO.setCurrentUserRole(currentUserRole);

        List<ProjectMemberVO> members=projectMemberMapper.selectMembersByProjectId(project.getId());
        detailVO.setMembers(members);

        ProjectTaskStatsVO taskStats=projectMapper.selectTaskStats(project.getId());
        if(taskStats==null){
            taskStats=emptyTaskStats();
        }
        detailVO.setTaskStats(taskStats);
        return detailVO;
    }

    private ProjectTaskStatsVO emptyTaskStats() {
        ProjectTaskStatsVO taskStats = new ProjectTaskStatsVO();
        taskStats.setTotal(0L);
        taskStats.setTodo(0L);
        taskStats.setInProgress(0L);
        taskStats.setInReview(0L);
        taskStats.setDone(0L);
        return taskStats;
    }

    private void checkOwner(Integer projectId, Integer currentUserId) {
        ProjectMember member = getProjectMemberOrThrow(projectId, currentUserId);
        if (!ROLE_OWNER.equals(member.getRole())) {
            throw new BusinessException(403, "你不是该项目的负责人，只有项目OWNER可以执行该操作");
        }
    }
}