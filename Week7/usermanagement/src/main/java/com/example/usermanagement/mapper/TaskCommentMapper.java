package com.example.usermanagement.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.usermanagement.entity.TaskComment;
import com.example.usermanagement.vo.TaskCommentVO;

@Mapper
public interface TaskCommentMapper extends BaseMapper<TaskComment> {
    // 查询任务的评论列表，按时间正序，并关联用户表获取用户名
    @Select("""
            SELECT
                tc.id,
                tc.task_id AS taskId,
                tc.user_id AS userId,
                u.username AS username,
                tc.content,
                tc.created_at AS createdAt
            FROM task_comment tc
            LEFT JOIN user u ON tc.user_id = u.id
            WHERE tc.task_id = #{taskId}
            ORDER BY tc.created_at ASC, tc.id ASC
            """)
    List<TaskCommentVO> selectCommentsByTaskId(@Param("taskId") Integer taskId);
    // 如果用INTER-->task_comment.user_id 允许因用户删除变成 NULL --> INNER JOIN user 会漏掉这类评论

    @Select("""
            SELECT
                tc.id,
                tc.task_id AS taskId,
                tc.user_id AS userId,
                u.username AS username,
                tc.content,
                tc.created_at AS createdAt
            FROM task_comment tc
            LEFT JOIN user u ON tc.user_id = u.id
            WHERE tc.id = #{id}
            """)
    TaskCommentVO selectCommentVOById(@Param("id") Integer id);
}
