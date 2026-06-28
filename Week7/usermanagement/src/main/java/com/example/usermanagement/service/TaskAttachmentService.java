package com.example.usermanagement.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.example.usermanagement.vo.TaskAttachmentDownloadVO;
import com.example.usermanagement.vo.TaskAttachmentVO;

public interface TaskAttachmentService {
    TaskAttachmentVO uploadAttachment(Integer taskId, MultipartFile file, Integer currentUserId);
    TaskAttachmentDownloadVO downloadAttachment(Integer attachmentId, Integer currentUserId);
    List<String> listStoredFilenamesByTaskId(Integer taskId);
    List<String> listStoredFilenamesByProjectId(Integer projectId);
    void deletePhysicalFiles(List<String> filenames);
}
