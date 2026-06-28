package com.example.usermanagement.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.usermanagement.entity.Task;
import com.example.usermanagement.vo.TaskVO;

/**
 * 使用script标签来实现动态SQL查询，支持根据不同条件进行任务列表的筛选。
 * 不在Service层用LambdaQueryWrapper-->LambdaQueryWrapper适合单表查询，只能返回Task entity里的字段，拿不到assigneeUsername（user table）
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {
    @Select("""
            <script>
            SELECT
                t.id,
                t.project_id AS projectId,
                t.title,
                t.description,
                t.status,
                t.priority,
                t.assignee_id AS assigneeId,
                u.username AS assigneeUsername,
                t.deadline,
                t.created_at AS createdAt,
                t.updated_at AS updatedAt
            FROM task t
            LEFT JOIN user u ON t.assignee_id = u.id
            WHERE t.project_id = #{projectId}
            <if test="status != null and status != ''">
                AND t.status = #{status}
            </if>
            <if test="priority != null and priority != ''">
                AND t.priority = #{priority}
            </if>
            <if test="assigneeId != null">
                AND t.assignee_id = #{assigneeId}
            </if>
            ORDER BY t.created_at DESC
            </script>
            """)
    Page<TaskVO> selectTaskPage(
            Page<TaskVO> page,
            @Param("projectId") Integer projectId,
            @Param("status") String status,
            @Param("priority") String priority,
            @Param("assigneeId") Integer assigneeId
    );

    @Select("""
            SELECT
                t.id,
                t.project_id AS projectId,
                t.title,
                t.description,
                t.status,
                t.priority,
                t.assignee_id AS assigneeId,
                u.username AS assigneeUsername,
                t.deadline,
                t.created_at AS createdAt,
                t.updated_at AS updatedAt
            FROM task t
            LEFT JOIN user u ON t.assignee_id = u.id
            WHERE t.id = #{id}
            """)
    TaskVO selectTaskVOById(@Param("id") Integer id);
}
