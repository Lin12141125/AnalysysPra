package com.example.usermanagement;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.usermanagement.cache.ProjectCacheManager;
import com.example.usermanagement.dto.ProjectMemberInviteDTO;
import com.example.usermanagement.entity.Project;
import com.example.usermanagement.entity.ProjectMember;
import com.example.usermanagement.entity.User;
import com.example.usermanagement.exception.BusinessException;
import com.example.usermanagement.mapper.ProjectMapper;
import com.example.usermanagement.mapper.ProjectMemberMapper;
import com.example.usermanagement.mapper.UserMapper;
import com.example.usermanagement.service.impl.ProjectServiceImpl;
import com.example.usermanagement.vo.ProjectMemberVO;
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
public class ProjectMemberServiceImplTest {

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ProjectMemberMapper projectMemberMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ProjectCacheManager projectCacheManager;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private Project project;
    private ProjectMember ownerMember;
    private ProjectMember memberMember;
    private ProjectMember viewerMember;

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

        memberMember = new ProjectMember();
        memberMember.setId(2);
        memberMember.setProjectId(1);
        memberMember.setUserId(2);
        memberMember.setRole("MEMBER");

        viewerMember = new ProjectMember();
        viewerMember.setId(3);
        viewerMember.setProjectId(1);
        viewerMember.setUserId(3);
        viewerMember.setRole("VIEWER");
    }

    @Test
    @DisplayName("邀请成员：OWNER可以邀请MEMBER")
    void inviteMemberShouldSucceedWhenCurrentUserIsOwner() {
        ProjectMemberInviteDTO dto = new ProjectMemberInviteDTO();
        dto.setUserId(4);
        dto.setRole("MEMBER");

        User invitedUser = new User();
        invitedUser.setId(4);
        invitedUser.setUsername("newMember");
        invitedUser.setEmail("newMember@example.com");

        ProjectMemberVO invitedMemberVO = new ProjectMemberVO();
        invitedMemberVO.setUserId(4);
        invitedMemberVO.setUsername("newMember");
        invitedMemberVO.setEmail("newMember@example.com");
        invitedMemberVO.setRole("MEMBER");

        when(projectMapper.selectById(1)).thenReturn(project);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(ownerMember)
                .thenReturn(null);
        when(userMapper.selectById(4)).thenReturn(invitedUser);
        when(projectMemberMapper.insert(any(ProjectMember.class))).thenReturn(1);
        when(projectMemberMapper.selectMembersByProjectId(1)).thenReturn(List.of(invitedMemberVO));

        ProjectMemberVO result = projectService.inviteMember(1, dto, 1);

        assertEquals(4, result.getUserId());
        assertEquals("MEMBER", result.getRole());
        verify(projectMemberMapper).insert(any(ProjectMember.class));
        verify(projectCacheManager).evictProject(1);
        verify(projectCacheManager).evictMyProjects(4);
    }

    @Test
    @DisplayName("邀请成员：OWNER可以邀请VIEWER")
    void inviteViewerShouldSucceedWhenCurrentUserIsOwner() {
        ProjectMemberInviteDTO dto = new ProjectMemberInviteDTO();
        dto.setUserId(4);
        dto.setRole("VIEWER");

        User invitedUser = new User();
        invitedUser.setId(4);
        invitedUser.setUsername("newViewer");
        invitedUser.setEmail("newViewer@example.com");

        ProjectMemberVO invitedMemberVO = new ProjectMemberVO();
        invitedMemberVO.setUserId(4);
        invitedMemberVO.setUsername("newViewer");
        invitedMemberVO.setEmail("newViewer@example.com");
        invitedMemberVO.setRole("VIEWER");

        when(projectMapper.selectById(1)).thenReturn(project);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(ownerMember)
                .thenReturn(null);
        when(userMapper.selectById(4)).thenReturn(invitedUser);
        when(projectMemberMapper.insert(any(ProjectMember.class))).thenReturn(1);
        when(projectMemberMapper.selectMembersByProjectId(1)).thenReturn(List.of(invitedMemberVO));

        ProjectMemberVO result = projectService.inviteMember(1, dto, 1);

        assertEquals(4, result.getUserId());
        assertEquals("VIEWER", result.getRole());
        verify(projectMemberMapper).insert(any(ProjectMember.class));
    }

    @Test
    @DisplayName("邀请成员：MEMBER不能邀请")
    void inviteMemberShouldThrowWhenCurrentUserIsNotOwner() {
        ProjectMemberInviteDTO dto = new ProjectMemberInviteDTO();
        dto.setUserId(4);
        dto.setRole("MEMBER");

        when(projectMapper.selectById(1)).thenReturn(project);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(memberMember);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> projectService.inviteMember(1, dto, 2));

        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("OWNER"));
        verify(projectMemberMapper, never()).insert(any(ProjectMember.class));
    }

    @Test
    @DisplayName("邀请成员：不能邀请OWNER角色")
    void inviteMemberShouldThrowWhenRoleIsOwner() {
        ProjectMemberInviteDTO dto = new ProjectMemberInviteDTO();
        dto.setUserId(4);
        dto.setRole("OWNER");

        when(projectMapper.selectById(1)).thenReturn(project);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ownerMember);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> projectService.inviteMember(1, dto, 1));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("MEMBER或VIEWER"));
        verify(projectMemberMapper, never()).insert(any(ProjectMember.class));
    }

    @Test
    @DisplayName("邀请成员：不能重复邀请已有成员")
    void inviteMemberShouldThrowWhenTargetUserAlreadyMember() {
        ProjectMemberInviteDTO dto = new ProjectMemberInviteDTO();
        dto.setUserId(2);
        dto.setRole("MEMBER");

        User invitedUser = new User();
        invitedUser.setId(2);
        invitedUser.setUsername("user");

        when(projectMapper.selectById(1)).thenReturn(project);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(ownerMember)
                .thenReturn(memberMember);
        when(userMapper.selectById(2)).thenReturn(invitedUser);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> projectService.inviteMember(1, dto, 1));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("已是项目成员"));
        verify(projectMemberMapper, never()).insert(any(ProjectMember.class));
    }

    @Test
    @DisplayName("邀请成员：被邀请用户不存在时返回404")
    void inviteMemberShouldThrowWhenInvitedUserDoesNotExist() {
        ProjectMemberInviteDTO dto = new ProjectMemberInviteDTO();
        dto.setUserId(99);
        dto.setRole("MEMBER");

        when(projectMapper.selectById(1)).thenReturn(project);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ownerMember);
        when(userMapper.selectById(99)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> projectService.inviteMember(1, dto, 1));

        assertEquals(404, exception.getCode());
        assertTrue(exception.getMessage().contains("被邀请用户不存在"));
        verify(projectMemberMapper, never()).insert(any(ProjectMember.class));
    }

    @Test
    @DisplayName("移除成员：OWNER可以移除MEMBER")
    void removeMemberShouldSucceedWhenCurrentUserIsOwner() {
        when(projectMapper.selectById(1)).thenReturn(project);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(ownerMember)
                .thenReturn(memberMember);
        when(projectMemberMapper.deleteById(2)).thenReturn(1);

        projectService.removeMember(1, 2, 1);

        verify(projectMemberMapper).deleteById(2);
        verify(projectCacheManager).evictProject(1);
        verify(projectCacheManager).evictMyProjects(2);
    }

    @Test
    @DisplayName("移除成员：MEMBER不能移除成员")
    void removeMemberShouldThrowWhenCurrentUserIsNotOwner() {
        when(projectMapper.selectById(1)).thenReturn(project);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(memberMember);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> projectService.removeMember(1, 3, 2));

        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("OWNER"));
        verify(projectMemberMapper, never()).deleteById(anyInt());
    }

    @Test
    @DisplayName("移除成员：不能移除OWNER")
    void removeMemberShouldThrowWhenTargetIsOwner() {
        when(projectMapper.selectById(1)).thenReturn(project);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(ownerMember)
                .thenReturn(ownerMember);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> projectService.removeMember(1, 1, 1));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("不能移除项目负责人"));
        verify(projectMemberMapper, never()).deleteById(anyInt());
    }

    @Test
    @DisplayName("查看成员列表：OWNER可以查看")
    void listMembersShouldSucceedWhenCurrentUserIsOwner() {
        when(projectMapper.selectById(1)).thenReturn(project);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ownerMember);
        when(projectMemberMapper.selectMembersByProjectId(1)).thenReturn(List.of(ownerVO(), memberVO()));

        List<ProjectMemberVO> result = projectService.listMembers(1, 1);

        assertEquals(2, result.size());
        assertEquals("OWNER", result.get(0).getRole());
    }

    @Test
    @DisplayName("查看成员列表：MEMBER可以查看")
    void listMembersShouldSucceedWhenCurrentUserIsMember() {
        when(projectMapper.selectById(1)).thenReturn(project);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(memberMember);
        when(projectMemberMapper.selectMembersByProjectId(1)).thenReturn(List.of(ownerVO(), memberVO()));

        List<ProjectMemberVO> result = projectService.listMembers(1, 2);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("查看成员列表：VIEWER不能查看")
    void listMembersShouldThrowWhenCurrentUserIsViewer() {
        when(projectMapper.selectById(1)).thenReturn(project);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(viewerMember);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> projectService.listMembers(1, 3));

        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("OWNER或MEMBER"));
        verify(projectMemberMapper, never()).selectMembersByProjectId(anyInt());
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
}