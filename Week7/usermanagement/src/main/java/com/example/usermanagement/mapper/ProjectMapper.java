package com.example.usermanagement.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.usermanagement.entity.Project;
import com.example.usermanagement.vo.ProjectListVO;
import com.example.usermanagement.vo.ProjectTaskStatsVO;

@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
    /**
     * 查询当前用户参与的项目列表，按创建时间降序排序
     * 需要关联查询project表和project_member表，以获取当前用户在每个项目中的角色信息-->project.id = project_member.project_id, project_member.user_id = 当前登录用户ID
     * Mybatis-Plus自动只能查单表
     */
    @Select("""
            SELECT p.id, p.name, p.description, p.owner_id AS ownerId, pm.role AS currentUserRole, p.created_at AS createdAt, p.updated_at AS updatedAt
            FROM project p
            INNER JOIN project_member pm ON p.id = pm.project_id
            WHERE pm.user_id = #{userId}
            ORDER BY p.created_at DESC
            """)
    Page<ProjectListVO> selectMyProjectsPage(Page<ProjectListVO> page, @Param("userId") Integer userId);

    /**
     * 统计task表，根据项目ID统计各个状态的任务数量，包括总数, todo, IN_PROGRESS、IN_REVIEW、DONE
     */
    @Select("""
            SELECT
                COUNT(*) AS total,
                COALESCE(SUM(CASE WHEN status = 'TODO' THEN 1 ELSE 0 END), 0) AS todo,
                COALESCE(SUM(CASE WHEN status = 'IN_PROGRESS' THEN 1 ELSE 0 END), 0) AS inProgress,
                COALESCE(SUM(CASE WHEN status = 'IN_REVIEW' THEN 1 ELSE 0 END), 0) AS inReview,
                COALESCE(SUM(CASE WHEN status = 'DONE' THEN 1 ELSE 0 END), 0) AS done
            FROM task
            WHERE project_id = #{projectId}
            """)
    ProjectTaskStatsVO selectTaskStats(@Param("projectId") Integer projectId);
}
