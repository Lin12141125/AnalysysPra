package com.example.usermanagement.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.usermanagement.common.Result;
import com.example.usermanagement.exception.BusinessException;
import com.example.usermanagement.security.SecurityUserDetails;
import com.example.usermanagement.service.TaskAttachmentService;
import com.example.usermanagement.vo.TaskAttachmentDownloadVO;
import com.example.usermanagement.vo.TaskAttachmentVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;

@RestController
@Validated
@Tag(name = "任务附件管理", description = "任务附件的上传和下载接口")
public class TaskAttachmentController {
    @Autowired
    private TaskAttachmentService taskAttachmentService;

    @PostMapping(value = "/api/tasks/{taskId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传任务附件", description = "上传附件到指定任务，只有项目的OWNER和MEMBER可以上传附件，VIEWER只能查看/下载附件")
    public Result<TaskAttachmentVO> uploadAttachment(
            @Parameter(description = "任务ID，必须为正整数", required = true, example = "1")
            @PathVariable @Min(1) Integer taskId,
            @Parameter(description = "附件文件，最大10M", required = true)
            @RequestParam("file") MultipartFile file) {
        Integer currentUserId = getCurrentUserId();
        TaskAttachmentVO attachment = taskAttachmentService.uploadAttachment(taskId, file, currentUserId);
        return Result.success(attachment);
    }

    @GetMapping("/api/attachments/{id}")
    @Operation(summary = "下载任务附件", description = "下载指定附件，项目成员均可下载，VIEWER允许只读下载")
    public ResponseEntity<Resource> downloadAttachment(
            @Parameter(description = "附件ID，必须为正整数", required = true, example = "1")
            @PathVariable @Min(1) Integer id) {
        Integer currentUserId = getCurrentUserId();
        TaskAttachmentDownloadVO download = taskAttachmentService.downloadAttachment(id, currentUserId);

        String encodedFilename = URLEncoder.encode(download.getOriginalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.getContentType()))
                .contentLength(download.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(download.getResource());
    }


    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUserDetails userDetails)) {
            throw new BusinessException(401, "用户未登录");
        }
        return userDetails.getUser().getId();
    }
}
