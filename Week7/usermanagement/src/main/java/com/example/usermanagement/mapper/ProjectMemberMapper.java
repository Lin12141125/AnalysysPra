package com.example.usermanagement.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.usermanagement.entity.ProjectMember;
import com.example.usermanagement.vo.ProjectMemberVO;

@Mapper
public interface ProjectMemberMapper extends BaseMapper<ProjectMember> {
    /**
     * 如果只查project_member表，只能拿到user_id和role，无法获取username和email等用户信息
     */
    @Select("""
            SELECT
                pm.user_id AS userId,
                u.username AS username,
                u.email AS email,
                pm.role AS role
            FROM project_member pm
            INNER JOIN user u ON pm.user_id = u.id
            WHERE pm.project_id = #{projectId}
            ORDER BY
                CASE pm.role
                    WHEN 'OWNER' THEN 1
                    WHEN 'MEMBER' THEN 2
                    WHEN 'VIEWER' THEN 3
                    ELSE 4
                END,
                pm.user_id ASC
            """)
    List<ProjectMemberVO> selectMembersByProjectId(@Param("projectId") Integer projectId);
}
