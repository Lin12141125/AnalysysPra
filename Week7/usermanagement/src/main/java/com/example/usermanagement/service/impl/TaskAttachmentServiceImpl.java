package com.example.usermanagement.service.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.usermanagement.entity.ProjectMember;
import com.example.usermanagement.entity.Task;
import com.example.usermanagement.entity.TaskAttachment;
import com.example.usermanagement.exception.BusinessException;
import com.example.usermanagement.mapper.ProjectMemberMapper;
import com.example.usermanagement.mapper.TaskAttachmentMapper;
import com.example.usermanagement.mapper.TaskMapper;
import com.example.usermanagement.service.TaskAttachmentService;
import com.example.usermanagement.vo.TaskAttachmentDownloadVO;
import com.example.usermanagement.vo.TaskAttachmentVO;

@Service
public class TaskAttachmentServiceImpl implements TaskAttachmentService {
    private static final String ROLE_VIEWER = "VIEWER";
    private static final long MAX_ATTACHMENT_SIZE = 10L * 1024 * 1024; // 10M
    private static final String ATTACHMENT_DIR = "task-attachments";

    @Autowired
    private TaskAttachmentMapper taskAttachmentMapper;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private ProjectMemberMapper projectMemberMapper;

    @Override
    @Transactional
    public TaskAttachmentVO uploadAttachment(Integer taskId, MultipartFile file, Integer currentUserId) {
        // 验证task存在性
        Task task = getTaskOrThrow(taskId);
        // 验证当前用户是项目成员
        ProjectMember currentMember = getProjectMemberOrThrow(task.getProjectId(), currentUserId);
        // 当前角色是viewer-->不能上传附件
        if (ROLE_VIEWER.equals(currentMember.getRole())) {
            throw new BusinessException(403, "VIEWER只能查看/下载附件，不能上传附件");
        }
        // 验证文件大小
        validateFile(file);
        
        // 保存文件到磁盘
        String originalName = getSafeOriginalName(file); // 获取原始名
        String extension = getExtension(originalName); // 获取扩展名
        String storedFilename = UUID.randomUUID().toString() + extension; // UUID + 扩展名，防止文件名冲突
        
        Path root=getAttachmentRootPath();
        Path destination = root.resolve(storedFilename).normalize();

        if(!destination.startsWith(root)) {
            throw new BusinessException(400, "文件路径不合法");
        }

        boolean fileSaved = false;
        try{
            Files.createDirectories(root);
            file.transferTo(destination.toFile());
            fileSaved = true;

            TaskAttachment attachment = new TaskAttachment();
            attachment.setTaskId(taskId);
            attachment.setFilename(storedFilename);
            attachment.setOriginalName(originalName);
            attachment.setFileSize(file.getSize());
            attachment.setUploadedBy(currentUserId);
            attachment.setCreatedAt(LocalDateTime.now());

            taskAttachmentMapper.insert(attachment);

            TaskAttachmentVO attachmentVO = taskAttachmentMapper.selectAttachmentVOById(attachment.getId());
            if (attachmentVO == null) {
                throw new BusinessException(404, "附件不存在，id=" + attachment.getId());
            }
            return attachmentVO;
        } catch (IOException e) {
            throw new BusinessException(500, "附件保存失败");
        } catch (RuntimeException e){
            if(fileSaved){
                try {
                    Files.deleteIfExists(destination);
                } catch (IOException ignored) {
                    // 忽略删除失败的异常
                }
            }
            throw e;
        }
    }

    @Override
    public TaskAttachmentDownloadVO downloadAttachment(Integer attachmentId, Integer currentUserId) {
        TaskAttachment attachment = getAttachmentOrThrow(attachmentId);
        Task task = getTaskOrThrow(attachment.getTaskId());
        getProjectMemberOrThrow(task.getProjectId(), currentUserId); // 验证当前用户是项目成员

        Path root = getAttachmentRootPath();
        Path filePath = root.resolve(attachment.getFilename()).normalize();

        if(!filePath.startsWith(root)){
            throw new BusinessException(500, "附件路径异常");
        }
        if(!Files.exists(filePath) || !Files.isReadable(filePath)){
            throw new BusinessException(404, "附件文件不存在或不可读");
        }

        try{
            Resource resource = new UrlResource(filePath.toUri());
            String contentType = Files.probeContentType(filePath);
            if(contentType == null || contentType.isBlank()){
                contentType = "application/octet-stream"; // 默认二进制流
            }
            return new TaskAttachmentDownloadVO(resource, attachment.getOriginalName(), attachment.getFileSize(), contentType);
        }catch (MalformedURLException e) {
            throw new BusinessException(500, "附件路径异常");
        } catch (IOException e) {
            throw new BusinessException(500, "读取附件失败");
        }
    }

    private Task getTaskOrThrow(Integer taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在，id=" + taskId);
        }
        return task;
    }

    private ProjectMember getProjectMemberOrThrow(Integer projectId, Integer userId) {
        LambdaQueryWrapper<ProjectMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId);
        ProjectMember member = projectMemberMapper.selectOne(queryWrapper);
        if (member == null) {
            throw new BusinessException(403, "用户不是项目成员，无权上传附件");
        }
        return member;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new BusinessException(400, "上传的文件不能为空");
        }
        if (file.getSize() > MAX_ATTACHMENT_SIZE) {
            throw new BusinessException(400, "附件大小不能超过10MB");
        }
    }

    private String getSafeOriginalName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String cleanedName=StringUtils.cleanPath(originalFilename == null ? "attachment" : originalFilename);
        String fileName=Paths.get(cleanedName).getFileName().toString();

        if(fileName.isBlank()) fileName="attachment"; // 如果文件名为空，使用默认名称
        if(fileName.length()>255){
            throw new BusinessException(400, "文件名过长，不能超过255个字符");
        }
        return fileName;
    }

    private String getExtension(String originalName){
        int lastDotIndex = originalName.lastIndexOf('.');
        if (lastDotIndex < 0 || lastDotIndex == originalName.length() - 1) {
            return ""; // 没有扩展名
        }
        String extension = originalName.substring(lastDotIndex);
        if (extension.length() > 20) {
            return ""; // 扩展名过长，忽略
        }
        return extension;
    }

    private Path getAttachmentRootPath() {
        return Paths.get(System.getProperty("user.dir"), "uploads", ATTACHMENT_DIR)
                .toAbsolutePath()
                .normalize();
    }

    private TaskAttachment getAttachmentOrThrow(Integer attachmentId) {
        TaskAttachment attachment = taskAttachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new BusinessException(404, "附件不存在，id=" + attachmentId);
        }
        return attachment;
    }
}
