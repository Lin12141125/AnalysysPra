package com.example.usermanagement.vo;

import lombok.Data;

/**
 * 项目任务统计信息（展示在项目详情ProjectDetail下）
 */
@Data
public class ProjectTaskStatsVO {
    private Long total;       // 项目下任务总数
    private Long todo;        // TODO状态任务数
    private Long inProgress;  // IN_PROGRESS状态任务数
    private Long inReview;    // IN_REVIEW状态任务数
    private Long done;        // DONE状态任务数
}
