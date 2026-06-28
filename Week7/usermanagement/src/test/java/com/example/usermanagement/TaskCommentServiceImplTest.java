package com.example.usermanagement;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.usermanagement.dto.TaskCommentCreateDTO;
import com.example.usermanagement.entity.ProjectMember;
import com.example.usermanagement.entity.Task;
import com.example.usermanagement.entity.TaskComment;
import com.example.usermanagement.exception.BusinessException;
import com.example.usermanagement.mapper.ProjectMemberMapper;
import com.example.usermanagement.mapper.TaskCommentMapper;
import com.example.usermanagement.mapper.TaskMapper;
import com.example.usermanagement.service.impl.TaskCommentServiceImpl;
import com.example.usermanagement.vo.TaskCommentVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TaskCommentServiceImplTest {

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private TaskCommentMapper taskCommentMapper;

    @Mock
    private ProjectMemberMapper projectMemberMapper;

    @InjectMocks
    private TaskCommentServiceImpl taskCommentService;

    private Task task;
    private ProjectMember ownerMember;
    private ProjectMember memberMember;
    private ProjectMember viewerMember;

    @BeforeEach
    void setUp() {
        task = new Task();
        task.setId(1);
        task.setProjectId(1);
        task.setTitle("task1");

        ownerMember = projectMember(1, "OWNER");
        memberMember = projectMember(2, "MEMBER");
        viewerMember = projectMember(3, "VIEWER");
    }

    @Test
    @DisplayName("任务8：OWNER可以添加任务评论")
    void addCommentShouldSucceedWhenCurrentUserIsOwner() {
        TaskCommentCreateDTO dto = new TaskCommentCreateDTO();
        dto.setContent("owner comment");

        when(taskMapper.selectById(1)).thenReturn(task);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ownerMember);
        when(taskCommentMapper.insert(any(TaskComment.class))).thenAnswer(invocation -> {
            TaskComment comment = invocation.getArgument(0);
            comment.setId(10);
            return 1;
        });
        when(taskCommentMapper.selectCommentVOById(10)).thenReturn(commentVO(10, 1, 1, "admin", "owner comment"));

        TaskCommentVO result = taskCommentService.addComment(1, dto, 1);

        assertEquals(10, result.getId());
        assertEquals("owner comment", result.getContent());
        assertEquals(1, result.getUserId());
        verify(taskCommentMapper).insert(any(TaskComment.class));
    }

    @Test
    @DisplayName("任务8：MEMBER可以添加任务评论")
    void addCommentShouldSucceedWhenCurrentUserIsMember() {
        TaskCommentCreateDTO dto = new TaskCommentCreateDTO();
        dto.setContent("member comment");

        when(taskMapper.selectById(1)).thenReturn(task);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(memberMember);
        when(taskCommentMapper.insert(any(TaskComment.class))).thenAnswer(invocation -> {
            TaskComment comment = invocation.getArgument(0);
            comment.setId(11);
            return 1;
        });
        when(taskCommentMapper.selectCommentVOById(11)).thenReturn(commentVO(11, 1, 2, "user", "member comment"));

        TaskCommentVO result = taskCommentService.addComment(1, dto, 2);

        assertEquals(11, result.getId());
        assertEquals("member comment", result.getContent());
        assertEquals(2, result.getUserId());
        verify(taskCommentMapper).insert(any(TaskComment.class));
    }

    @Test
    @DisplayName("任务8：VIEWER不能添加任务评论")
    void addCommentShouldThrowWhenCurrentUserIsViewer() {
        TaskCommentCreateDTO dto = new TaskCommentCreateDTO();
        dto.setContent("viewer comment");

        when(taskMapper.selectById(1)).thenReturn(task);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(viewerMember);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> taskCommentService.addComment(1, dto, 3));

        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("VIEWER"));
        verify(taskCommentMapper, never()).insert(any(TaskComment.class));
    }

    @Test
    @DisplayName("任务8：非项目成员不能添加任务评论")
    void addCommentShouldThrowWhenCurrentUserIsNotProjectMember() {
        TaskCommentCreateDTO dto = new TaskCommentCreateDTO();
        dto.setContent("outsider comment");

        when(taskMapper.selectById(1)).thenReturn(task);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> taskCommentService.addComment(1, dto, 99));

        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("不是项目成员"));
        verify(taskCommentMapper, never()).insert(any(TaskComment.class));
    }

    @Test
    @DisplayName("任务8：项目成员可以按时间正序查看评论列表")
    void listCommentsShouldReturnCommentsForProjectMember() {
        List<TaskCommentVO> comments = List.of(
                commentVO(1, 1, 1, "admin", "first comment"),
                commentVO(2, 1, 2, "user", "second comment")
        );

        when(taskMapper.selectById(1)).thenReturn(task);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(viewerMember);
        when(taskCommentMapper.selectCommentsByTaskId(1)).thenReturn(comments);

        List<TaskCommentVO> result = taskCommentService.listComments(1, 3);

        assertEquals(2, result.size());
        assertEquals("first comment", result.get(0).getContent());
        assertEquals("second comment", result.get(1).getContent());
        verify(taskCommentMapper).selectCommentsByTaskId(1);
    }

    @Test
    @DisplayName("任务8：非项目成员不能查看评论列表")
    void listCommentsShouldThrowWhenCurrentUserIsNotProjectMember() {
        when(taskMapper.selectById(1)).thenReturn(task);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> taskCommentService.listComments(1, 99));

        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("不是项目成员"));
        verify(taskCommentMapper, never()).selectCommentsByTaskId(anyInt());
    }

    @Test
    @DisplayName("任务8：MEMBER可以删除自己的评论")
    void deleteCommentShouldSucceedWhenMemberDeletesOwnComment() {
        TaskComment comment = comment(1, 1, 2, "own comment");

        when(taskCommentMapper.selectById(1)).thenReturn(comment);
        when(taskMapper.selectById(1)).thenReturn(task);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(memberMember);
        when(taskCommentMapper.deleteById(1)).thenReturn(1);

        taskCommentService.deleteComment(1, 2);

        verify(taskCommentMapper).deleteById(1);
    }

    @Test
    @DisplayName("任务8：MEMBER不能删除他人的评论")
    void deleteCommentShouldThrowWhenMemberDeletesOthersComment() {
        TaskComment comment = comment(1, 1, 1, "owner comment");

        when(taskCommentMapper.selectById(1)).thenReturn(comment);
        when(taskMapper.selectById(1)).thenReturn(task);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(memberMember);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> taskCommentService.deleteComment(1, 2));

        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("只能删除自己"));
        verify(taskCommentMapper, never()).deleteById(anyInt());
    }

    @Test
    @DisplayName("任务8：OWNER可以删除任意评论")
    void deleteCommentShouldSucceedWhenOwnerDeletesAnyComment() {
        TaskComment comment = comment(1, 1, 2, "member comment");

        when(taskCommentMapper.selectById(1)).thenReturn(comment);
        when(taskMapper.selectById(1)).thenReturn(task);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ownerMember);
        when(taskCommentMapper.deleteById(1)).thenReturn(1);

        taskCommentService.deleteComment(1, 1);

        verify(taskCommentMapper).deleteById(1);
    }

    @Test
    @DisplayName("任务8：VIEWER不能删除评论")
    void deleteCommentShouldThrowWhenViewerDeletesComment() {
        TaskComment comment = comment(1, 1, 3, "viewer comment");

        when(taskCommentMapper.selectById(1)).thenReturn(comment);
        when(taskMapper.selectById(1)).thenReturn(task);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(viewerMember);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> taskCommentService.deleteComment(1, 3));

        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("VIEWER"));
        verify(taskCommentMapper, never()).deleteById(anyInt());
    }

    @Test
    @DisplayName("任务8：删除评论时不存在评论应返回404")
    void deleteCommentShouldThrowWhenCommentDoesNotExist() {
        when(taskCommentMapper.selectById(99)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> taskCommentService.deleteComment(99, 1));

        assertEquals(404, exception.getCode());
        assertTrue(exception.getMessage().contains("评论不存在"));
        verify(taskMapper, never()).selectById(anyInt());
    }

    private ProjectMember projectMember(Integer userId, String role) {
        ProjectMember member = new ProjectMember();
        member.setProjectId(1);
        member.setUserId(userId);
        member.setRole(role);
        return member;
    }

    private TaskComment comment(Integer id, Integer taskId, Integer userId, String content) {
        TaskComment comment = new TaskComment();
        comment.setId(id);
        comment.setTaskId(taskId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setCreatedAt(LocalDateTime.now());
        return comment;
    }

    private TaskCommentVO commentVO(Integer id, Integer taskId, Integer userId, String username, String content) {
        TaskCommentVO commentVO = new TaskCommentVO();
        commentVO.setId(id);
        commentVO.setTaskId(taskId);
        commentVO.setUserId(userId);
        commentVO.setUsername(username);
        commentVO.setContent(content);
        commentVO.setCreatedAt("2026-06-28 16:00:00");
        return commentVO;
    }
}