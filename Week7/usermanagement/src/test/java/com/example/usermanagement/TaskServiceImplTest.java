package com.example.usermanagement;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.usermanagement.cache.TaskCacheManager;
import com.example.usermanagement.dto.TaskCreateDTO;
import com.example.usermanagement.dto.TaskStatusUpdateDTO;
import com.example.usermanagement.dto.TaskUpdateDTO;
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
public class TaskServiceImplTest {

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
    private ProjectMember ownerMember;
    private ProjectMember normalMember;
    private ProjectMember viewerMember;
    private ProjectMember otherMember;
    private Task assignedTask;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(1);
        project.setName("project1");

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

        viewerMember = new ProjectMember();
        viewerMember.setId(3);
        viewerMember.setProjectId(1);
        viewerMember.setUserId(3);
        viewerMember.setRole("VIEWER");

        otherMember = new ProjectMember();
        otherMember.setId(4);
        otherMember.setProjectId(1);
        otherMember.setUserId(4);
        otherMember.setRole("MEMBER");

        assignedTask = new Task();
        assignedTask.setId(1);
        assignedTask.setProjectId(1);
        assignedTask.setTitle("task1");
        assignedTask.setDescription("task1 description");
        assignedTask.setStatus("TODO");
        assignedTask.setPriority("MEDIUM");
        assignedTask.setAssigneeId(2);
        assignedTask.setCreatedAt(LocalDateTime.now());
        assignedTask.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("创建任务：OWNER可以创建并默认TODO状态")
    void createTaskShouldSucceedWhenCurrentUserIsOwner() {
        TaskCreateDTO dto = new TaskCreateDTO();
        dto.setTitle("task6");
        dto.setDescription("task6 description");
        dto.setPriority("HIGH");
        dto.setAssigneeId(2);

        User assignee = new User();
        assignee.setId(2);
        assignee.setUsername("user");

        when(projectMapper.selectById(1)).thenReturn(project);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(ownerMember)
                .thenReturn(normalMember);
        when(userMapper.selectById(2)).thenReturn(assignee);
        when(taskMapper.insert(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(6);
            return 1;
        });
        when(taskMapper.selectTaskVOById(6)).thenReturn(taskVO(6, "task6", "TODO", "HIGH", 2));

        TaskVO result = taskService.createTask(1, dto, 1);

        assertEquals(6, result.getId());
        assertEquals("TODO", result.getStatus());
        assertEquals("HIGH", result.getPriority());
        assertEquals(2, result.getAssigneeId());
        verify(taskMapper).insert(any(Task.class));
        verify(taskCacheManager).evictTaskList(1);
    }

    @Test
    @DisplayName("创建任务：VIEWER不能创建")
    void createTaskShouldThrowWhenCurrentUserIsViewer() {
        TaskCreateDTO dto = new TaskCreateDTO();
        dto.setTitle("task6");

        when(projectMapper.selectById(1)).thenReturn(project);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(viewerMember);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> taskService.createTask(1, dto, 3));

        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("VIEWER"));
        verify(taskMapper, never()).insert(any(Task.class));
    }

    @Test
    @DisplayName("创建任务：负责人必须是项目成员")
    void createTaskShouldThrowWhenAssigneeIsNotProjectMember() {
        TaskCreateDTO dto = new TaskCreateDTO();
        dto.setTitle("task6");
        dto.setAssigneeId(99);

        User assignee = new User();
        assignee.setId(99);

        when(projectMapper.selectById(1)).thenReturn(project);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(ownerMember)
                .thenReturn(null);
        when(userMapper.selectById(99)).thenReturn(assignee);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> taskService.createTask(1, dto, 1));

        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("不是项目成员"));
        verify(taskMapper, never()).insert(any(Task.class));
    }

    @Test
    @DisplayName("查询任务列表：项目成员可按状态优先级负责人筛选")
    void listTasksShouldReturnFilteredPageWhenCurrentUserIsProjectMember() {
        mockTaskPageCachePassThrough();

        Page<TaskVO> pageResult = new Page<>(1, 10);
        pageResult.setTotal(1);
        pageResult.setRecords(List.of(taskVO(1, "task1", "TODO", "HIGH", 2)));

        when(projectMapper.selectById(1)).thenReturn(project);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(normalMember);
        when(taskMapper.selectTaskPage(any(Page.class), eq(1), eq("TODO"), eq("HIGH"), eq(2))).thenReturn(pageResult);

        Page<TaskVO> result = taskService.listTasks(1, "TODO", "HIGH", 2, 1, 10, 2);

        assertEquals(1, result.getTotal());
        assertEquals("task1", result.getRecords().get(0).getTitle());
        verify(taskCacheManager).queryTaskPage(eq(1), eq(1), eq(10), eq("TODO"), eq("HIGH"), eq(2), any());
    }

    @Test
    @DisplayName("查询任务列表：非法状态应返回400")
    void listTasksShouldThrowWhenStatusIsInvalid() {
        when(projectMapper.selectById(1)).thenReturn(project);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(normalMember);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> taskService.listTasks(1, "INVALID", null, null, 1, 10, 2));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("任务状态"));
        verify(taskMapper, never()).selectTaskPage(any(Page.class), anyInt(), any(), any(), any());
    }

    @Test
    @DisplayName("更新任务：OWNER可以更新任意任务")
    void updateTaskShouldSucceedWhenCurrentUserIsOwner() {
        TaskUpdateDTO dto = new TaskUpdateDTO();
        dto.setTitle("task1-updated");
        dto.setPriority("URGENT");

        when(taskMapper.selectById(1)).thenReturn(assignedTask);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ownerMember);
        when(taskMapper.updateById(any(Task.class))).thenReturn(1);
        when(taskMapper.selectTaskVOById(1)).thenReturn(taskVO(1, "task1-updated", "TODO", "URGENT", 2));

        TaskVO result = taskService.updateTask(1, dto, 1);

        assertEquals("task1-updated", result.getTitle());
        assertEquals("URGENT", result.getPriority());
        verify(taskMapper).updateById(any(Task.class));
        verify(taskCacheManager).evictTaskList(1);
    }

    @Test
    @DisplayName("更新任务：MEMBER可以更新自己负责的任务")
    void updateTaskShouldSucceedWhenMemberIsAssignee() {
        TaskUpdateDTO dto = new TaskUpdateDTO();
        dto.setTitle("task1-member-updated");

        when(taskMapper.selectById(1)).thenReturn(assignedTask);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(normalMember);
        when(taskMapper.updateById(any(Task.class))).thenReturn(1);
        when(taskMapper.selectTaskVOById(1)).thenReturn(taskVO(1, "task1-member-updated", "TODO", "MEDIUM", 2));

        TaskVO result = taskService.updateTask(1, dto, 2);

        assertEquals("task1-member-updated", result.getTitle());
        verify(taskMapper).updateById(any(Task.class));
        verify(taskCacheManager).evictTaskList(1);
    }

    @Test
    @DisplayName("更新任务：MEMBER不能更新非自己负责的任务")
    void updateTaskShouldThrowWhenMemberIsNotAssignee() {
        TaskUpdateDTO dto = new TaskUpdateDTO();
        dto.setTitle("should-fail");

        when(taskMapper.selectById(1)).thenReturn(assignedTask);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(otherMember);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> taskService.updateTask(1, dto, 4));

        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("任务负责人"));
        verify(taskMapper, never()).updateById(any(Task.class));
    }

    @Test
    @DisplayName("删除任务：OWNER可以删除任意任务")
    void deleteTaskShouldSucceedWhenCurrentUserIsOwner() {
        when(taskMapper.selectById(1)).thenReturn(assignedTask);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ownerMember);
        when(taskMapper.deleteById(1)).thenReturn(1);

        taskService.deleteTask(1, 1);

        verify(taskMapper).deleteById(1);
        verify(taskCacheManager).evictTaskList(1);
    }

    @Test
    @DisplayName("删除任务：MEMBER不能删除非自己负责的任务")
    void deleteTaskShouldThrowWhenMemberIsNotAssignee() {
        when(taskMapper.selectById(1)).thenReturn(assignedTask);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(otherMember);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> taskService.deleteTask(1, 4));

        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("任务负责人"));
        verify(taskMapper, never()).deleteById(anyInt());
    }

    @Test
    @DisplayName("状态流转：OWNER可以将TODO流转为IN_PROGRESS")
    void updateTaskStatusShouldTransitTodoToInProgressWhenCurrentUserIsOwner() {
        TaskStatusUpdateDTO dto = new TaskStatusUpdateDTO();
        dto.setStatus("IN_PROGRESS");

        when(taskMapper.selectById(1)).thenReturn(assignedTask);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ownerMember);
        when(taskMapper.updateById(any(Task.class))).thenReturn(1);
        when(taskMapper.selectTaskVOById(1)).thenReturn(taskVO(1, "task1", "IN_PROGRESS", "MEDIUM", 2));

        TaskVO result = taskService.updateTaskStatus(1, dto, 1);

        assertEquals("IN_PROGRESS", result.getStatus());
        verify(taskMapper).updateById(any(Task.class));
        verify(taskCacheManager).evictTaskList(1);
    }

    @Test
    @DisplayName("状态流转：任务负责人可以将IN_PROGRESS流转为IN_REVIEW")
    void updateTaskStatusShouldTransitInProgressToInReviewWhenMemberIsAssignee() {
        assignedTask.setStatus("IN_PROGRESS");
        TaskStatusUpdateDTO dto = new TaskStatusUpdateDTO();
        dto.setStatus("IN_REVIEW");

        when(taskMapper.selectById(1)).thenReturn(assignedTask);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(normalMember);
        when(taskMapper.updateById(any(Task.class))).thenReturn(1);
        when(taskMapper.selectTaskVOById(1)).thenReturn(taskVO(1, "task1", "IN_REVIEW", "MEDIUM", 2));

        TaskVO result = taskService.updateTaskStatus(1, dto, 2);

        assertEquals("IN_REVIEW", result.getStatus());
        verify(taskMapper).updateById(any(Task.class));
        verify(taskCacheManager).evictTaskList(1);
    }

    @Test
    @DisplayName("状态流转：OWNER可以将IN_REVIEW流转为DONE")
    void updateTaskStatusShouldTransitInReviewToDoneWhenCurrentUserIsOwner() {
        assignedTask.setStatus("IN_REVIEW");
        TaskStatusUpdateDTO dto = new TaskStatusUpdateDTO();
        dto.setStatus("DONE");

        when(taskMapper.selectById(1)).thenReturn(assignedTask);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ownerMember);
        when(taskMapper.updateById(any(Task.class))).thenReturn(1);
        when(taskMapper.selectTaskVOById(1)).thenReturn(taskVO(1, "task1", "DONE", "MEDIUM", 2));

        TaskVO result = taskService.updateTaskStatus(1, dto, 1);

        assertEquals("DONE", result.getStatus());
        verify(taskMapper).updateById(any(Task.class));
        verify(taskCacheManager).evictTaskList(1);
    }

    @Test
    @DisplayName("状态流转：不能从TODO直接流转为DONE")
    void updateTaskStatusShouldThrowWhenTransitSkipsIntermediateStatus() {
        TaskStatusUpdateDTO dto = new TaskStatusUpdateDTO();
        dto.setStatus("DONE");

        when(taskMapper.selectById(1)).thenReturn(assignedTask);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ownerMember);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> taskService.updateTaskStatus(1, dto, 1));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("顺序流转"));
        verify(taskMapper, never()).updateById(any(Task.class));
        verify(taskCacheManager, never()).evictTaskList(anyInt());
    }

    @Test
    @DisplayName("状态流转：DONE状态不能继续流转")
    void updateTaskStatusShouldThrowWhenCurrentStatusIsDone() {
        assignedTask.setStatus("DONE");
        TaskStatusUpdateDTO dto = new TaskStatusUpdateDTO();
        dto.setStatus("DONE");

        when(taskMapper.selectById(1)).thenReturn(assignedTask);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ownerMember);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> taskService.updateTaskStatus(1, dto, 1));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("DONE流转到DONE"));
        verify(taskMapper, never()).updateById(any(Task.class));
        verify(taskCacheManager, never()).evictTaskList(anyInt());
    }

    @Test
    @DisplayName("状态流转：非OWNER且非任务负责人不能更新状态")
    void updateTaskStatusShouldThrowWhenCurrentUserCannotModifyTask() {
        TaskStatusUpdateDTO dto = new TaskStatusUpdateDTO();
        dto.setStatus("IN_PROGRESS");

        when(taskMapper.selectById(1)).thenReturn(assignedTask);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(otherMember);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> taskService.updateTaskStatus(1, dto, 4));

        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("任务负责人"));
        verify(taskMapper, never()).updateById(any(Task.class));
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
        taskVO.setDescription(title + " description");
        taskVO.setStatus(status);
        taskVO.setPriority(priority);
        taskVO.setAssigneeId(assigneeId);
        taskVO.setAssigneeUsername(assigneeId == null ? null : "user");
        taskVO.setCreatedAt(LocalDateTime.now());
        taskVO.setUpdatedAt(LocalDateTime.now());
        return taskVO;
    }
}