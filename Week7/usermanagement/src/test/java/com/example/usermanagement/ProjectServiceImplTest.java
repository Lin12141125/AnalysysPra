package com.example.usermanagement;

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
import com.example.usermanagement.service.TaskAttachmentService;
import com.example.usermanagement.service.impl.ProjectServiceImpl;
import com.example.usermanagement.vo.ProjectDetailVO;
import com.example.usermanagement.vo.ProjectListVO;
import com.example.usermanagement.vo.ProjectMemberVO;
import com.example.usermanagement.vo.ProjectTaskStatsVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProjectServiceImplTest {

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ProjectMemberMapper projectMemberMapper;

    @Mock
    private ProjectCacheManager projectCacheManager;

    @Mock
    private TaskAttachmentService taskAttachmentService;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private Project project;
    private ProjectMember ownerMember;
    private ProjectMember normalMember;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(1);
        project.setName("project1");
        project.setDescription("project1 description");
        project.setOwnerId(1);
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());

        ownerMember = new ProjectMember();
        ownerMember.setId(1);
        ownerMember.setProjectId(1);
        ownerMember.setUserId(1);
        ownerMember.setRole("OWNER");

        normalMember = new ProjectMember();
        normalMember.setId(2);
        normalMember.setProjectId(1);
        normalMember.setUserId(2);
        normalMember.setRole("MEMBER");
    }

    @Test
    @DisplayName("创建项目：应插入项目并自动创建OWNER成员")
    void createProjectShouldInsertProjectAndOwnerMember() {
        mockProjectDetailCachePassThrough();

        ProjectCreateDTO dto = new ProjectCreateDTO();
        dto.setName("project3");
        dto.setDescription("project3 description");

        when(projectMapper.insert(any(Project.class))).thenAnswer(invocation -> {
            Project insertedProject = invocation.getArgument(0);
            insertedProject.setId(3);
            return 1;
        });
        when(projectMemberMapper.insert(any(ProjectMember.class))).thenReturn(1);

        Project savedProject = new Project();
        savedProject.setId(3);
        savedProject.setName("project3");
        savedProject.setDescription("project3 description");
        savedProject.setOwnerId(1);
        savedProject.setCreatedAt(LocalDateTime.now());
        savedProject.setUpdatedAt(LocalDateTime.now());

        ProjectMember savedOwnerMember = new ProjectMember();
        savedOwnerMember.setId(3);
        savedOwnerMember.setProjectId(3);
        savedOwnerMember.setUserId(1);
        savedOwnerMember.setRole("OWNER");

        when(projectMapper.selectById(3)).thenReturn(savedProject);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(savedOwnerMember);
        when(projectMemberMapper.selectMembersByProjectId(3)).thenReturn(List.of(ownerVO()));
        when(projectMapper.selectTaskStats(3)).thenReturn(emptyStats());

        ProjectDetailVO result = projectService.createProject(dto, 1);

        assertEquals(3, result.getId());
        assertEquals("project3", result.getName());
        assertEquals("OWNER", result.getCurrentUserRole());

        verify(projectMapper).insert(any(Project.class));
        verify(projectMemberMapper).insert(any(ProjectMember.class));
        verify(projectCacheManager).evictMyProjects(1);
    }

    @Test
    @DisplayName("查询我参与的项目：应通过缓存入口返回分页数据")
    void listMyProjectsShouldReturnPagedProjects() {
        mockProjectListCachePassThrough();

        Page<ProjectListVO> pageResult = new Page<>(1, 10);
        ProjectListVO projectListVO = new ProjectListVO();
        projectListVO.setId(1);
        projectListVO.setName("project1");
        projectListVO.setDescription("project1 description");
        projectListVO.setOwnerId(1);
        projectListVO.setCurrentUserRole("OWNER");
        pageResult.setRecords(List.of(projectListVO));
        pageResult.setTotal(1);

        when(projectMapper.selectMyProjectsPage(any(Page.class), eq(1))).thenReturn(pageResult);

        Page<ProjectListVO> result = projectService.listMyProjects(1, 1, 10);

        assertEquals(1, result.getTotal());
        assertEquals("project1", result.getRecords().get(0).getName());
        assertEquals("OWNER", result.getRecords().get(0).getCurrentUserRole());
        verify(projectCacheManager).queryMyProjects(eq(1), eq(1), eq(10), any());
    }

    @Test
    @DisplayName("查看项目详情：项目成员可以查看成员列表和任务统计")
    void getProjectDetailShouldReturnDetailWhenUserIsMember() {
        mockProjectDetailCachePassThrough();

        when(projectMapper.selectById(1)).thenReturn(project);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(normalMember);
        when(projectMemberMapper.selectMembersByProjectId(1)).thenReturn(List.of(ownerVO(), memberVO()));
        when(projectMapper.selectTaskStats(1)).thenReturn(sampleStats());

        ProjectDetailVO result = projectService.getProjectDetail(1, 2);

        assertEquals(1, result.getId());
        assertEquals("project1", result.getName());
        assertEquals("MEMBER", result.getCurrentUserRole());
        assertEquals(2, result.getMembers().size());
        assertEquals(4L, result.getTaskStats().getTotal());
        assertEquals(1L, result.getTaskStats().getDone());
    }

    @Test
    @DisplayName("查看项目详情：非项目成员应抛出403")
    void getProjectDetailShouldThrowWhenUserIsNotProjectMember() {
        mockProjectDetailCachePassThrough();

        when(projectMapper.selectById(1)).thenReturn(project);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> projectService.getProjectDetail(1, 99));

        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("不是该项目成员"));
        verify(projectMemberMapper, never()).selectMembersByProjectId(anyInt());
    }

    @Test
    @DisplayName("更新项目：OWNER可以更新并清理项目相关缓存")
    void updateProjectShouldSucceedWhenUserIsOwner() {
        mockProjectDetailCachePassThrough();

        ProjectUpdateDTO dto = new ProjectUpdateDTO();
        dto.setName("project1-updated");
        dto.setDescription("project1 updated description");

        when(projectMapper.selectById(1)).thenReturn(project);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ownerMember);
        when(projectMapper.updateById(any(Project.class))).thenReturn(1);
        when(projectMemberMapper.selectMembersByProjectId(1)).thenReturn(List.of(ownerVO(), memberVO()));
        when(projectMapper.selectTaskStats(1)).thenReturn(sampleStats());

        ProjectDetailVO result = projectService.updateProject(1, dto, 1);

        assertEquals("project1-updated", result.getName());
        assertEquals("project1 updated description", result.getDescription());
        assertEquals("OWNER", result.getCurrentUserRole());
        verify(projectMapper).updateById(any(Project.class));
        verify(projectCacheManager).evictProject(1);
        verify(projectCacheManager).evictMyProjects(1);
        verify(projectCacheManager).evictMyProjects(2);
    }

    @Test
    @DisplayName("更新项目：MEMBER不能更新")
    void updateProjectShouldThrowWhenUserIsNotOwner() {
        ProjectUpdateDTO dto = new ProjectUpdateDTO();
        dto.setName("project1-updated");

        when(projectMapper.selectById(1)).thenReturn(project);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(normalMember);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> projectService.updateProject(1, dto, 2));

        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("OWNER"));
        verify(projectMapper, never()).updateById(any(Project.class));
        verify(projectCacheManager, never()).evictProject(anyInt());
    }

    @Test
    @DisplayName("删除项目：OWNER可以删除并清理项目相关缓存")
    void deleteProjectShouldSucceedWhenUserIsOwner() {
        when(projectMapper.selectById(1)).thenReturn(project);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ownerMember);
        when(projectMemberMapper.selectMembersByProjectId(1)).thenReturn(List.of(ownerVO(), memberVO()));
        when(taskAttachmentService.listStoredFilenamesByProjectId(1)).thenReturn(List.of("project-file.txt", "project-image.jpg"));
        when(projectMapper.deleteById(1)).thenReturn(1);

        projectService.deleteProject(1, 1);

        verify(taskAttachmentService).listStoredFilenamesByProjectId(1);
        verify(projectMapper).deleteById(1);
        verify(taskAttachmentService).deletePhysicalFiles(List.of("project-file.txt", "project-image.jpg"));
        verify(projectCacheManager).evictProject(1);
        verify(projectCacheManager).evictMyProjects(1);
        verify(projectCacheManager).evictMyProjects(2);
    }

    @Test
    @DisplayName("删除项目：MEMBER不能删除")
    void deleteProjectShouldThrowWhenUserIsNotOwner() {
        when(projectMapper.selectById(1)).thenReturn(project);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(normalMember);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> projectService.deleteProject(1, 2));

        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("OWNER"));
        verify(projectMapper, never()).deleteById(anyInt());
        verify(taskAttachmentService, never()).listStoredFilenamesByProjectId(anyInt());
        verify(taskAttachmentService, never()).deletePhysicalFiles(any());
    }

    private void mockProjectDetailCachePassThrough() {
        when(projectCacheManager.queryProjectDetail(anyInt(), anyInt(), any())).thenAnswer(invocation -> {
            Supplier<ProjectDetailVO> dbLoader = invocation.getArgument(2);
            return dbLoader.get();
        });
    }

    private void mockProjectListCachePassThrough() {
        when(projectCacheManager.queryMyProjects(anyInt(), anyInt(), anyInt(), any())).thenAnswer(invocation -> {
            Supplier<Page<ProjectListVO>> dbLoader = invocation.getArgument(3);
            return dbLoader.get();
        });
    }

    private ProjectMemberVO ownerVO() {
        ProjectMemberVO vo = new ProjectMemberVO();
        vo.setUserId(1);
        vo.setUsername("admin");
        vo.setEmail("admin@example.com");
        vo.setRole("OWNER");
        return vo;
    }

    private ProjectMemberVO memberVO() {
        ProjectMemberVO vo = new ProjectMemberVO();
        vo.setUserId(2);
        vo.setUsername("user");
        vo.setEmail("user@example.com");
        vo.setRole("MEMBER");
        return vo;
    }

    private ProjectTaskStatsVO sampleStats() {
        ProjectTaskStatsVO stats = new ProjectTaskStatsVO();
        stats.setTotal(4L);
        stats.setTodo(1L);
        stats.setInProgress(1L);
        stats.setInReview(1L);
        stats.setDone(1L);
        return stats;
    }

    private ProjectTaskStatsVO emptyStats() {
        ProjectTaskStatsVO stats = new ProjectTaskStatsVO();
        stats.setTotal(0L);
        stats.setTodo(0L);
        stats.setInProgress(0L);
        stats.setInReview(0L);
        stats.setDone(0L);
        return stats;
    }
}