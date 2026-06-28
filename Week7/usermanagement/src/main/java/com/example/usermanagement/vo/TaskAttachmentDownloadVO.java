package com.example.usermanagement.vo;

import org.springframework.core.io.Resource;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaskAttachmentDownloadVO {
    private Resource resource;
    private String originalName;
    private long fileSize;
    private String contentType;
}
