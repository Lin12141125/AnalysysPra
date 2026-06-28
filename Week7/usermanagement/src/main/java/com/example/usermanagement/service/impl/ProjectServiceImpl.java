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
import com.example.usermanagement.dto.ProjectMemberInviteDTO;
import com.example.usermanagement.dto.ProjectUpdateDTO;
import com.example.usermanagement.entity.Project;
import com.example.usermanagement.entity.ProjectMember;
import com.example.usermanagement.entity.User;
import com.example.usermanagement.exception.BusinessException;
import com.example.usermanagement.mapper.ProjectMapper;
import com.example.usermanagement.mapper.ProjectMemberMapper;
import com.example.usermanagement.mapper.UserMapper;
import com.example.usermanagement.service.ProjectService;
import com.example.usermanagement.vo.ProjectDetailVO;
import com.example.usermanagement.vo.ProjectListVO;
import com.example.usermanagement.vo.ProjectMemberVO;
import com.example.usermanagement.vo.ProjectTaskStatsVO;

@Service
public class ProjectServiceImpl implements ProjectService {
    private static final String ROLE_OWNER = "OWNER";
    private static final String ROLE_MEMBER = "MEMBER";
    private static final String ROLE_VIEWER = "VIEWER";

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ProjectMemberMapper projectMemberMapper;

    @Autowired
    private ProjectCacheManager projectCacheManager;

    @Autowired
    private UserMapper userMapper;

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

    @Override
    @Transactional
    public ProjectMemberVO inviteMember(Integer projectId, ProjectMemberInviteDTO dto, Integer currentUserId) {
        getProjectOrThrow(projectId);
        checkOwner(projectId, currentUserId); // 校验当前用户是OWNER

        if(!ROLE_MEMBER.equals(dto.getRole()) && !ROLE_VIEWER.equals(dto.getRole())) {
            throw new BusinessException(400, "邀请的项目成员角色只能是MEMBER或VIEWER");
        }

        User invitedUser = userMapper.selectById(dto.getUserId());
        if (invitedUser == null) {
            throw new BusinessException(404, "被邀请用户不存在，id=" + dto.getUserId());
        }

        ProjectMember existingMember = getProjectMember(projectId, dto.getUserId());
        if (existingMember != null) {
            throw new BusinessException(400, "该用户已是项目成员，不能重复邀请");
        }

        ProjectMember newMember = new ProjectMember();
        newMember.setProjectId(projectId);
        newMember.setUserId(dto.getUserId());
        newMember.setRole(dto.getRole());
        projectMemberMapper.insert(newMember);

        projectCacheManager.evictProject(projectId); // 成员变化-->清除项目详情缓存
        projectCacheManager.evictMyProjects(dto.getUserId()); // 清除被邀请用户的项目列表缓存

        return getProjectMemberVOOrThrow(projectId, dto.getUserId());
    }

    @Override
    @Transactional
    public void removeMember(Integer projectId, Integer targetUserId, Integer currentUserId){
        getProjectOrThrow(projectId);
        checkOwner(projectId, currentUserId); // 校验当前用户是OWNER

        ProjectMember targetMember = getProjectMember(projectId, targetUserId);
        if (targetMember == null) {
            throw new BusinessException(404, "该用户不是项目成员");
        }
        if (ROLE_OWNER.equals(targetMember.getRole())) {
            throw new BusinessException(400, "不能移除项目负责人");
        }

        projectMemberMapper.deleteById(targetMember.getId());

        projectCacheManager.evictProject(projectId);
        projectCacheManager.evictMyProjects(targetUserId);
    }

    @Override
    public List<ProjectMemberVO> listMembers(Integer projectId, Integer currentUserId) {
        getProjectOrThrow(projectId);
        ProjectMember currentMember = getProjectMemberOrThrow(projectId, currentUserId);
        if (!ROLE_OWNER.equals(currentMember.getRole()) && !ROLE_MEMBER.equals(currentMember.getRole())) {
            throw new BusinessException(403, "只有项目OWNER或MEMBER可以查看成员列表");
        }
        
        return projectMemberMapper.selectMembersByProjectId(projectId);
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

    private ProjectMemberVO getProjectMemberVOOrThrow(Integer projectId, Integer userId) {
        List<ProjectMemberVO> members = projectMemberMapper.selectMembersByProjectId(projectId);
        return members.stream()
                .filter(member -> userId.equals(member.getUserId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(404, "项目成员不存在"));
    }
}