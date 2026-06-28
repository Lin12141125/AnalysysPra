package com.example.usermanagement.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.usermanagement.entity.TaskAttachment;
import com.example.usermanagement.vo.TaskAttachmentVO;

@Mapper
public interface TaskAttachmentMapper extends BaseMapper<TaskAttachment> {
    @Select("""
            SELECT
                ta.id,
                ta.task_id AS taskId,
                ta.filename,
                ta.original_name AS originalName,
                ta.file_size AS fileSize,
                ta.uploaded_by AS uploadedBy,
                u.username AS uploadedByUsername,
                ta.created_at AS createdAt
            FROM task_attachment ta
            LEFT JOIN user u ON ta.uploaded_by = u.id
            WHERE ta.id = #{id}
            """)
    TaskAttachmentVO selectAttachmentVOById(@Param("id") Integer id);
}
