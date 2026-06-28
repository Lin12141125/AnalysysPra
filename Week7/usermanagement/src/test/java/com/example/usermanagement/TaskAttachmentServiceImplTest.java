package com.example.usermanagement;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.usermanagement.entity.ProjectMember;
import com.example.usermanagement.entity.Task;
import com.example.usermanagement.entity.TaskAttachment;
import com.example.usermanagement.exception.BusinessException;
import com.example.usermanagement.mapper.ProjectMemberMapper;
import com.example.usermanagement.mapper.TaskAttachmentMapper;
import com.example.usermanagement.mapper.TaskMapper;
import com.example.usermanagement.service.impl.TaskAttachmentServiceImpl;
import com.example.usermanagement.vo.TaskAttachmentDownloadVO;
import com.example.usermanagement.vo.TaskAttachmentVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TaskAttachmentServiceImplTest {

    private static final Path TEST_IMAGE_PATH = Paths.get("src", "test", "QQ图片20260621054733.jpg");
    private static final Path WEEK7_TEXT_PATH = Paths.get("src", "test", "Week7.txt");
    private static final long WEEK7_TEXT_SIZE = 6477L;
    private static final long TEST_IMAGE_SIZE = 87518L;

    @Mock
    private TaskAttachmentMapper taskAttachmentMapper;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private ProjectMemberMapper projectMemberMapper;

    @InjectMocks
    private TaskAttachmentServiceImpl taskAttachmentService;

    private Task task;
    private ProjectMember ownerMember;
    private ProjectMember memberMember;
    private ProjectMember viewerMember;
    private final List<Path> filesToDelete = new ArrayList<>();

    @BeforeEach
    void setUp() {
        task = new Task();
        task.setId(1);
        task.setProjectId(1);
        task.setTitle("task1");

        ownerMember = projectMember(1, "OWNER");
        memberMember = projectMember(2, "MEMBER");
        viewerMember = projectMember(4, "VIEWER");
    }

    @AfterEach
    void tearDown() throws IOException {
        for (Path file : filesToDelete) {
            Files.deleteIfExists(file);
        }
    }

    @Test
    @DisplayName("任务9：OWNER可以上传JPG附件并写入附件记录")
    void uploadAttachmentShouldSaveJpgAndInsertRecordWhenOwnerUploads() throws IOException {
        byte[] imageBytes = Files.readAllBytes(TEST_IMAGE_PATH);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "QQ图片20260621054733.jpg",
                "image/jpeg",
                imageBytes
        );
        TaskAttachment[] insertedAttachment = new TaskAttachment[1];

        when(taskMapper.selectById(1)).thenReturn(task);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ownerMember);
        when(taskAttachmentMapper.insert(any(TaskAttachment.class))).thenAnswer(invocation -> {
            TaskAttachment attachment = invocation.getArgument(0);
            attachment.setId(20);
            insertedAttachment[0] = attachment;
            filesToDelete.add(attachmentRoot().resolve(attachment.getFilename()));
            return 1;
        });
        when(taskAttachmentMapper.selectAttachmentVOById(20)).thenAnswer(invocation -> attachmentVO(insertedAttachment[0], "admin"));

        TaskAttachmentVO result = taskAttachmentService.uploadAttachment(1, file, 1);

        assertEquals(20, result.getId());
        assertEquals(1, result.getTaskId());
        assertEquals("QQ图片20260621054733.jpg", result.getOriginalName());
        assertEquals(TEST_IMAGE_SIZE, result.getFileSize());
        assertEquals(1, result.getUploadedBy());
        assertNotEquals(result.getOriginalName(), result.getFilename());
        assertTrue(result.getFilename().endsWith(".jpg"));
        assertTrue(Files.exists(attachmentRoot().resolve(result.getFilename())));
        verify(taskAttachmentMapper).insert(any(TaskAttachment.class));
    }

    @Test
    @DisplayName("任务9：MEMBER可以上传附件")
    void uploadAttachmentShouldSucceedWhenMemberUploads() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "member-note.txt",
                "text/plain",
                "member upload".getBytes(StandardCharsets.UTF_8)
        );
        TaskAttachment[] insertedAttachment = new TaskAttachment[1];

        when(taskMapper.selectById(1)).thenReturn(task);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(memberMember);
        when(taskAttachmentMapper.insert(any(TaskAttachment.class))).thenAnswer(invocation -> {
            TaskAttachment attachment = invocation.getArgument(0);
            attachment.setId(21);
            insertedAttachment[0] = attachment;
            filesToDelete.add(attachmentRoot().resolve(attachment.getFilename()));
            return 1;
        });
        when(taskAttachmentMapper.selectAttachmentVOById(21)).thenAnswer(invocation -> attachmentVO(insertedAttachment[0], "member"));

        TaskAttachmentVO result = taskAttachmentService.uploadAttachment(1, file, 2);

        assertEquals(21, result.getId());
        assertEquals("member-note.txt", result.getOriginalName());
        assertEquals(2, result.getUploadedBy());
        assertTrue(Files.exists(attachmentRoot().resolve(result.getFilename())));
    }

    @Test
    @DisplayName("任务9：VIEWER不能上传附件")
    void uploadAttachmentShouldThrowWhenViewerUploads() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "viewer-note.txt",
                "text/plain",
                "viewer upload".getBytes(StandardCharsets.UTF_8)
        );

        when(taskMapper.selectById(1)).thenReturn(task);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(viewerMember);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> taskAttachmentService.uploadAttachment(1, file, 4));

        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("VIEWER"));
        verify(taskAttachmentMapper, never()).insert(any(TaskAttachment.class));
    }

    @Test
    @DisplayName("任务9：空文件不能上传")
    void uploadAttachmentShouldThrowWhenFileIsEmpty() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        when(taskMapper.selectById(1)).thenReturn(task);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ownerMember);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> taskAttachmentService.uploadAttachment(1, file, 1));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("不能为空"));
        verify(taskAttachmentMapper, never()).insert(any(TaskAttachment.class));
    }

    @Test
    @DisplayName("任务9：超过10MB的文件不能上传")
    void uploadAttachmentShouldThrowWhenFileExceeds10MB() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "too-large.bin",
                "application/octet-stream",
                new byte[10 * 1024 * 1024 + 1]
        );

        when(taskMapper.selectById(1)).thenReturn(task);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ownerMember);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> taskAttachmentService.uploadAttachment(1, file, 1));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("10MB"));
        verify(taskAttachmentMapper, never()).insert(any(TaskAttachment.class));
    }

    @Test
    @DisplayName("任务9：VIEWER可以下载SQL种子附件Week7.txt")
    void downloadAttachmentShouldReturnSeedWeek7TextForViewer() throws IOException {
        Path seedFile = attachmentRoot().resolve("Week7.txt");
        Files.createDirectories(seedFile.getParent());
        Files.copy(WEEK7_TEXT_PATH, seedFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        filesToDelete.add(seedFile);

        TaskAttachment attachment = attachment(1, 1, "Week7.txt", "Week7.txt", WEEK7_TEXT_SIZE, 1);

        when(taskAttachmentMapper.selectById(1)).thenReturn(attachment);
        when(taskMapper.selectById(1)).thenReturn(task);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(viewerMember);

        TaskAttachmentDownloadVO result = taskAttachmentService.downloadAttachment(1, 4);

        assertEquals("Week7.txt", result.getOriginalName());
        assertEquals(WEEK7_TEXT_SIZE, result.getFileSize());
        assertNotNull(result.getContentType());
        assertTrue(result.getResource().exists());
        assertEquals(WEEK7_TEXT_SIZE, result.getResource().contentLength());
        String content = Files.readString(seedFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("任务 9： 任务附件上传"));
    }

    @Test
    @DisplayName("任务9：非项目成员不能下载附件")
    void downloadAttachmentShouldThrowWhenUserIsNotProjectMember() {
        TaskAttachment attachment = attachment(1, 1, "Week7.txt", "Week7.txt", WEEK7_TEXT_SIZE, 1);

        when(taskAttachmentMapper.selectById(1)).thenReturn(attachment);
        when(taskMapper.selectById(1)).thenReturn(task);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> taskAttachmentService.downloadAttachment(1, 99));

        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("不是项目成员"));
    }

    @Test
    @DisplayName("任务9：附件元数据存在但磁盘文件缺失时返回404")
    void downloadAttachmentShouldThrowWhenPhysicalFileIsMissing() {
        TaskAttachment attachment = attachment(2, 1, "missing-file.txt", "missing-file.txt", 12L, 1);

        when(taskAttachmentMapper.selectById(2)).thenReturn(attachment);
        when(taskMapper.selectById(1)).thenReturn(task);
        when(projectMemberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ownerMember);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> taskAttachmentService.downloadAttachment(2, 1));

        assertEquals(404, exception.getCode());
        assertTrue(exception.getMessage().contains("附件文件不存在"));
    }

    @Test
    @DisplayName("任务9：不存在的附件返回404")
    void downloadAttachmentShouldThrowWhenAttachmentDoesNotExist() {
        when(taskAttachmentMapper.selectById(99)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> taskAttachmentService.downloadAttachment(99, 1));

        assertEquals(404, exception.getCode());
        assertTrue(exception.getMessage().contains("附件不存在"));
        verify(taskMapper, never()).selectById(anyInt());
    }

    private ProjectMember projectMember(Integer userId, String role) {
        ProjectMember member = new ProjectMember();
        member.setProjectId(1);
        member.setUserId(userId);
        member.setRole(role);
        return member;
    }

    private TaskAttachment attachment(Integer id, Integer taskId, String filename, String originalName, Long fileSize, Integer uploadedBy) {
        TaskAttachment attachment = new TaskAttachment();
        attachment.setId(id);
        attachment.setTaskId(taskId);
        attachment.setFilename(filename);
        attachment.setOriginalName(originalName);
        attachment.setFileSize(fileSize);
        attachment.setUploadedBy(uploadedBy);
        attachment.setCreatedAt(LocalDateTime.now());
        return attachment;
    }

    private TaskAttachmentVO attachmentVO(TaskAttachment attachment, String uploadedByUsername) {
        TaskAttachmentVO attachmentVO = new TaskAttachmentVO();
        attachmentVO.setId(attachment.getId());
        attachmentVO.setTaskId(attachment.getTaskId());
        attachmentVO.setFilename(attachment.getFilename());
        attachmentVO.setOriginalName(attachment.getOriginalName());
        attachmentVO.setFileSize(attachment.getFileSize());
        attachmentVO.setUploadedBy(attachment.getUploadedBy());
        attachmentVO.setUploadedByUsername(uploadedByUsername);
        attachmentVO.setCreatedAt(attachment.getCreatedAt());
        return attachmentVO;
    }

    private Path attachmentRoot() {
        return Paths.get(System.getProperty("user.dir"), "uploads", "task-attachments")
                .toAbsolutePath()
                .normalize();
    }
}