package com.example.usermanagement;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.usermanagement.cache.TaskCacheManager;
import com.example.usermanagement.dto.TaskAssignDTO;
import com.example.usermanagement.entity.Project;
import com.example.usermanagement.entity.ProjectMember;
import com.example.usermanagement.entity.Task;
import com.example.usermanagement.entity.User;
import com.example.usermanagement.exception.BusinessException;
import com.example.usermanagement.mapper.ProjectMapper;
import com.example.usermanagement.mapper.ProjectMemberMapper;
import com.example.usermanagement.mapper.TaskMapper;
import com.example.usermanagement.mapper.UserMapper;
import com.example.usermanagement.service.impl.TaskServiceImpl;
import com.example.usermanagement.vo.TaskVO;
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
public class TaskAssignmentServiceImplTest {

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ProjectMemberMapper projectMemberMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private TaskCacheManager taskCacheManager;

    @InjectMocks
    private TaskServiceImpl taskService;

    private Project project;
    private Task task;
    private ProjectMember ownerMember;
    private ProjectMember assigneeMember;
    private ProjectMember otherMember;
    private ProjectMember viewerMember;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(1);
        project.setName("project1");

        task = new Task();
        task.setId(1);
        task.setProjectId(1);
        task.setTitle("task1");
        task.setStatus("TODO");
        task.setPriority("MEDIUM");
        task.setAssigneeId(2);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        ownerMember = projectMember(1, "OWNER");
        assigneeMember = projectMember(2, "MEMBER");
        viewerMember = projectMember(3, "VIEWER");
        otherMember = projectMember(4, "MEMBER");
    }

    @Test
    @DisplayName("任务7：OWNER可以将任务重新分配给项目成员")
    void assignTaskShouldSucceedWhenOwnerAssignsProjectMember() {
        TaskAssignDTO dto = new TaskAssignDTO();
        dto.setAssigneeId(4);

        User assignee = new User();
        assignee.setId(4);
        assignee.setUsername("member4");

        when(taskMapper.selectById(1)).thenReturn(task);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(ownerMember)
                .thenReturn(otherMember);
        when(userMapper.selectById(4)).thenReturn(assignee);
        when(taskMapper.updateById(any(Task.class))).thenReturn(1);
        when(taskMapper.selectTaskVOById(1)).thenReturn(taskVO(1, "task1", "TODO", "MEDIUM", 4));

        TaskVO result = taskService.assignTask(1, dto, 1);

        assertEquals(4, result.getAssigneeId());
        verify(taskMapper).updateById(any(Task.class));
        verify(taskCacheManager).evictTaskList(1);
    }

    @Test
    @DisplayName("任务7：OWNER不能将任务分配给非项目成员")
    void assignTaskShouldThrowWhenAssigneeIsNotProjectMember() {
        TaskAssignDTO dto = new TaskAssignDTO();
        dto.setAssigneeId(99);

        User assignee = new User();
        assignee.setId(99);

        when(taskMapper.selectById(1)).thenReturn(task);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(ownerMember)
                .thenReturn(null);
        when(userMapper.selectById(99)).thenReturn(assignee);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> taskService.assignTask(1, dto, 1));

        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("不是项目成员"));
        verify(taskMapper, never()).updateById(any(Task.class));
        verify(taskCacheManager, never()).evictTaskList(anyInt());
    }

    @Test
    @DisplayName("任务7：OWNER不能将任务分配给不存在的用户")
    void assignTaskShouldThrowWhenAssigneeUserDoesNotExist() {
        TaskAssignDTO dto = new TaskAssignDTO();
        dto.setAssigneeId(99);

        when(taskMapper.selectById(1)).thenReturn(task);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ownerMember);
        when(userMapper.selectById(99)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> taskService.assignTask(1, dto, 1));

        assertEquals(404, exception.getCode());
        assertTrue(exception.getMessage().contains("用户不存在"));
        verify(taskMapper, never()).updateById(any(Task.class));
    }

    @Test
    @DisplayName("任务7：当前负责人MEMBER不能重新分配任务")
    void assignTaskShouldThrowWhenAssigneeMemberTriesToReassign() {
        TaskAssignDTO dto = new TaskAssignDTO();
        dto.setAssigneeId(4);

        when(taskMapper.selectById(1)).thenReturn(task);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(assigneeMember);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> taskService.assignTask(1, dto, 2));

        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("只有项目OWNER可以分配任务负责人"));
        verify(userMapper, never()).selectById(anyInt());
        verify(taskMapper, never()).updateById(any(Task.class));
    }

    @Test
    @DisplayName("任务7：VIEWER不能分配任务")
    void assignTaskShouldThrowWhenViewerTriesToAssign() {
        TaskAssignDTO dto = new TaskAssignDTO();
        dto.setAssigneeId(4);

        when(taskMapper.selectById(1)).thenReturn(task);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(viewerMember);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> taskService.assignTask(1, dto, 3));

        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("只有项目OWNER可以分配任务负责人"));
        verify(userMapper, never()).selectById(anyInt());
        verify(taskMapper, never()).updateById(any(Task.class));
    }

    @Test
    @DisplayName("任务7：任务列表支持按负责人和优先级筛选")
    void listTasksShouldFilterByPriorityAndAssignee() {
        mockTaskPageCachePassThrough();

        Page<TaskVO> pageResult = new Page<>(1, 10);
        pageResult.setTotal(1);
        pageResult.setRecords(List.of(taskVO(1, "task1", "TODO", "HIGH", 4)));

        when(projectMapper.selectById(1)).thenReturn(project);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ownerMember);
        when(taskMapper.selectTaskPage(any(Page.class), eq(1), eq(null), eq("HIGH"), eq(4))).thenReturn(pageResult);

        Page<TaskVO> result = taskService.listTasks(1, null, "HIGH", 4, 1, 10, 1);

        assertEquals(1, result.getTotal());
        assertEquals("HIGH", result.getRecords().get(0).getPriority());
        assertEquals(4, result.getRecords().get(0).getAssigneeId());
        verify(taskCacheManager).queryTaskPage(eq(1), eq(1), eq(10), eq(null), eq("HIGH"), eq(4), any());
    }

    @Test
    @DisplayName("任务7：非法优先级筛选应返回400")
    void listTasksShouldThrowWhenPriorityIsInvalid() {
        when(projectMapper.selectById(1)).thenReturn(project);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ownerMember);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> taskService.listTasks(1, null, "INVALID", null, 1, 10, 1));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("任务优先级"));
        verify(taskMapper, never()).selectTaskPage(any(Page.class), anyInt(), any(), any(), any());
        verify(taskCacheManager, never()).queryTaskPage(anyInt(), anyInt(), anyInt(), any(), any(), any(), any());
    }

    private ProjectMember projectMember(Integer userId, String role) {
        ProjectMember member = new ProjectMember();
        member.setProjectId(1);
        member.setUserId(userId);
        member.setRole(role);
        return member;
    }

    private void mockTaskPageCachePassThrough() {
        when(taskCacheManager.queryTaskPage(anyInt(), anyInt(), anyInt(), any(), any(), any(), any())).thenAnswer(invocation -> {
            Supplier<Page<TaskVO>> dbLoader = invocation.getArgument(6);
            return dbLoader.get();
        });
    }

    private TaskVO taskVO(Integer id, String title, String status, String priority, Integer assigneeId) {
        TaskVO taskVO = new TaskVO();
        taskVO.setId(id);
        taskVO.setProjectId(1);
        taskVO.setTitle(title);
        taskVO.setStatus(status);
        taskVO.setPriority(priority);
        taskVO.setAssigneeId(assigneeId);
        taskVO.setAssigneeUsername(assigneeId == null ? null : "user" + assigneeId);
        taskVO.setCreatedAt(LocalDateTime.now());
        taskVO.setUpdatedAt(LocalDateTime.now());
        return taskVO;
    }
}