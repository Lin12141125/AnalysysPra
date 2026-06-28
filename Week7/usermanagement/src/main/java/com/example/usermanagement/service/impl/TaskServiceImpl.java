package com.example.usermanagement.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.usermanagement.cache.TaskCacheManager;
import com.example.usermanagement.dto.TaskAssignDTO;
import com.example.usermanagement.dto.TaskCreateDTO;
import com.example.usermanagement.dto.TaskStatusUpdateDTO;
import com.example.usermanagement.dto.TaskUpdateDTO;
import com.example.usermanagement.entity.Project;
import com.example.usermanagement.entity.ProjectMember;
import com.example.usermanagement.entity.Task;
import com.example.usermanagement.entity.User;
import com.example.usermanagement.enums.TaskStatus;
import com.example.usermanagement.exception.BusinessException;
import com.example.usermanagement.mapper.ProjectMapper;
import com.example.usermanagement.mapper.ProjectMemberMapper;
import com.example.usermanagement.mapper.TaskMapper;
import com.example.usermanagement.mapper.UserMapper;
import com.example.usermanagement.service.TaskAttachmentService;
import com.example.usermanagement.service.TaskService;
import com.example.usermanagement.vo.TaskVO;

@Service
public class TaskServiceImpl implements TaskService {
    private static final String ROLE_OWNER = "OWNER";
    private static final String ROLE_MEMBER = "MEMBER";
    private static final String ROLE_VIEWER = "VIEWER";
    private static final String STATUS_TODO = TaskStatus.TODO.getCode();
    private static final String DEFAULT_PRIORITY = "MEDIUM";

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ProjectMemberMapper projectMemberMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TaskCacheManager taskCacheManager;

    @Autowired
    private TaskAttachmentService taskAttachmentService;

    @Override
    @Transactional
    public TaskVO createTask(Integer projectId, TaskCreateDTO dto, Integer currentUserId) {
        getProjectOrThrow(projectId);
        ProjectMember currentMember=getProjectMemberOrThrow(projectId, currentUserId);
        
        if(ROLE_VIEWER.equals(currentMember.getRole())){
            throw new BusinessException(403, "VIEWER只能查看任务，不能创建任务");
        }

        if(dto.getAssigneeId() !=null){
            checkAssigneeIsProjectMember(projectId, dto.getAssigneeId());
        }

        LocalDateTime now = LocalDateTime.now();
        Task task = new Task();
        task.setProjectId(projectId);
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setStatus(STATUS_TODO);
        task.setPriority(dto.getPriority() != null ? dto.getPriority() : DEFAULT_PRIORITY);
        task.setAssigneeId(dto.getAssigneeId());
        task.setDeadline(dto.getDeadline());
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        taskMapper.insert(task);
        taskCacheManager.evictTaskList(projectId);
        return getTaskVOOrThrow(task.getId());
    }

    @Override
    public Page<TaskVO> listTasks(
            Integer projectId,
            String status,
            String priority,
            Integer assigneeId,
            Integer page,
            Integer size,
            Integer currentUserId) {
        getProjectOrThrow(projectId);
        getProjectMemberOrThrow(projectId, currentUserId);

        validateStatusFilter(status);
        validatePriorityFilter(priority);

        return taskCacheManager.queryTaskPage(
                projectId,
                page,
                size,
                status,
                priority,
                assigneeId,
                () -> taskMapper.selectTaskPage(new Page<>(page, size), projectId, status, priority, assigneeId)
        );
    }

    @Override
    @Transactional
    public TaskVO updateTask(Integer taskId, TaskUpdateDTO dto, Integer currentUserId){
        Task task=getTaskOrThrow(taskId);
        ProjectMember currentMember = getProjectMemberOrThrow(task.getProjectId(), currentUserId);

        checkCanModifyTask(task, currentMember, currentUserId);
        // assign逻辑转至单独接口
        // if(dto.getAssigneeId() !=null){
        //     checkAssigneeIsProjectMember(task.getProjectId(), dto.getAssigneeId());
        //     task.setAssigneeId(dto.getAssigneeId());
        // }
        if(dto.getTitle() !=null) task.setTitle(dto.getTitle());
        if(dto.getDescription() !=null) task.setDescription(dto.getDescription());
        if(dto.getPriority() !=null) task.setPriority(dto.getPriority());
        if(dto.getDeadline() !=null) task.setDeadline(dto.getDeadline());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        taskCacheManager.evictTaskList(task.getProjectId());

        return getTaskVOOrThrow(task.getId());
    }

    @Override
    @Transactional
    public void deleteTask(Integer taskId, Integer currentUserId){
        Task task=getTaskOrThrow(taskId);
        ProjectMember currentMember = getProjectMemberOrThrow(task.getProjectId(), currentUserId);

        checkCanModifyTask(task, currentMember, currentUserId);
    List<String> attachmentFilenames = taskAttachmentService.listStoredFilenamesByTaskId(taskId);
        taskMapper.deleteById(taskId);
        taskAttachmentService.deletePhysicalFiles(attachmentFilenames);
        taskCacheManager.evictTaskList(task.getProjectId());
    }

    @Override
    @Transactional
    public TaskVO updateTaskStatus(Integer taskId, TaskStatusUpdateDTO dto, Integer currentUserId) {
        Task task = getTaskOrThrow(taskId);
        ProjectMember currentMember = getProjectMemberOrThrow(task.getProjectId(), currentUserId);

        checkCanModifyTask(task, currentMember, currentUserId);

        TaskStatus currentStatus=parseTaskStatus(task.getStatus()); // 当前任务状态
        TaskStatus targetStatus=parseTaskStatus(dto.getStatus()); // 目标任务状态

        // 检查状态流转是否合法
        if(!currentStatus.canTransitTo(targetStatus)){
            throw new BusinessException(400, "任务状态只能按TODO->IN_PROGRESS->IN_REVIEW->DONE的顺序流转,不能从" + currentStatus.getCode() + "流转到" + targetStatus.getCode());
        }
        // 若合法则更新状态
        task.setStatus(targetStatus.getCode());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        // 清除缓存
        taskCacheManager.evictTaskList(task.getProjectId());

        return getTaskVOOrThrow(task.getId());
    }

    @Override
    @Transactional
    public TaskVO assignTask(Integer taskId, TaskAssignDTO dto, Integer currentUserId){
        Task task=getTaskOrThrow(taskId);
        ProjectMember currentMember = getProjectMemberOrThrow(task.getProjectId(), currentUserId);

        checkCanAssignTask(currentMember);
        checkAssigneeIsProjectMember(task.getProjectId(), dto.getAssigneeId());

        task.setAssigneeId(dto.getAssigneeId());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        taskCacheManager.evictTaskList(task.getProjectId());

        return getTaskVOOrThrow(task.getId());
    }

    private Project getProjectOrThrow(Integer projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(404, "项目不存在，id=" + projectId);
        }
        return project;
    }

    private ProjectMember getProjectMemberOrThrow(Integer projectId, Integer userId) {
        ProjectMember member = getProjectMember(projectId, userId);
        if (member == null) {
            throw new BusinessException(403, "用户不是项目成员，无权访问项目任务" );
        }
        return member;  
    }

    private ProjectMember getProjectMember(Integer projectId, Integer userId) {
        LambdaQueryWrapper<ProjectMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProjectMember::getProjectId, projectId)
                    .eq(ProjectMember::getUserId, userId);
        return projectMemberMapper.selectOne(queryWrapper);
    }

    private void checkAssigneeIsProjectMember(Integer projectId, Integer assigneeId) {
        User user = userMapper.selectById(assigneeId);
        if (user == null) { // 用户不存在
            throw new BusinessException(404, "用户不存在，id=" + assigneeId);
        }

        ProjectMember assigneeMember = getProjectMember(projectId, assigneeId);
        if (assigneeMember == null) { // 被指派人不是项目成员
            throw new BusinessException(403, "被指派人不是项目成员，任务只能分配给该项目内的成员");
        }
    }

    private TaskVO getTaskVOOrThrow(Integer taskId) {
        TaskVO taskVO=taskMapper.selectTaskVOById(taskId);
        if(taskVO==null){
            throw new BusinessException(404, "任务不存在，id=" + taskId);
        }
        return taskVO;
    }

    private void validateStatusFilter(String status) {
        if (status==null || status.isBlank()) return;
        if(!"TODO".equals(status) && !"IN_PROGRESS".equals(status) && !"IN_REVIEW".equals(status) && !"DONE".equals(status)){
            throw new BusinessException(400, "任务状态只能是TODO、IN_PROGRESS、IN_REVIEW或DONE");
        }
    }

    private void validatePriorityFilter(String priority) {
        if (priority==null || priority.isBlank()) return;
        if(!"LOW".equals(priority) && !"MEDIUM".equals(priority) && !"HIGH".equals(priority) && !"URGENT".equals(priority)){
            throw new BusinessException(400, "任务优先级只能是LOW、MEDIUM、HIGH或URGENT");
        }
    }

    private Task getTaskOrThrow(Integer taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在，id=" + taskId);
        }
        return task;
    }

    private void checkCanModifyTask(Task task, ProjectMember currentMember, Integer currentUserId) {
        if(ROLE_OWNER.equals(currentMember.getRole())){
            return; // 项目拥有者可以修改任何任务
        }
        if(ROLE_MEMBER.equals(currentMember.getRole()) && task.getAssigneeId() != null && task.getAssigneeId().equals(currentUserId)){
            return; // 项目成员可以修改自己被指派的任务
        }
        throw new BusinessException(403, "只有项目OWNER或任务负责人可以修改/删除该任务");
    }

    private void checkCanAssignTask(ProjectMember currentMember) {
        if (ROLE_OWNER.equals(currentMember.getRole())) {
            return;
        }
        throw new BusinessException(403, "只有项目OWNER可以分配任务负责人");
    }

    private TaskStatus parseTaskStatus(String status) {
        try {
            return TaskStatus.fromCode(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, "任务状态无效，任务状态只能是TODO、IN_PROGRESS、IN_REVIEW或DONE");
        }
    }
}
