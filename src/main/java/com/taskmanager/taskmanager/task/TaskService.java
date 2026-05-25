package com.taskmanager.taskmanager.task;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.taskmanager.taskmanager.common.PageResponse;
import com.taskmanager.taskmanager.config.AuthUtils;
import com.taskmanager.taskmanager.exception.ResourceNotFoundException;
import com.taskmanager.taskmanager.exception.UnauthorizedException;
import com.taskmanager.taskmanager.task.dto.TaskFilterRequest;
import com.taskmanager.taskmanager.task.dto.TaskRequest;
import com.taskmanager.taskmanager.task.dto.TaskResponse;
import com.taskmanager.taskmanager.user.Role;
import com.taskmanager.taskmanager.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor // auto generates constructor for final fields by lombork
public class TaskService {
    private final TaskRepository taskRepository;
    private final AuthUtils authUtils;
    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    // map entity to dto
    private TaskResponse toResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .userEmail(task.getUser().getEmail())
                .userName(task.getUser().getName())
                .build();
    }

    private PageResponse<TaskResponse> toPageResponse(Page<Task> page) {
        return PageResponse.<TaskResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    private static final List<String> ALLOWED_SORT_FIELDS = List.of("createdAt", "updatedAt", "title", "status");

    public PageResponse<TaskResponse> getAllTasks(TaskFilterRequest filter) {
        User currentUser = authUtils.getCurrentUser();
        log.debug("Fetching tasks for user={} role={} filter={}",
                currentUser.getEmail(), currentUser.getRole(), filter.getStatus());

        if (!ALLOWED_SORT_FIELDS.contains(filter.getSortBy())) {
            filter.setSortBy("createdAt"); // fallback to default
        }
        Sort sort = filter.getSortBy().equalsIgnoreCase("asc") ? Sort.by(filter.getSortBy()).ascending()
                : Sort.by(filter.getSortBy()).descending();
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);
        Specification<Task> spec = Specification.where(TaskSpecification.hasStatus(filter.getStatus()))
                .and(TaskSpecification.titleOrDescriptionContains(filter.getKeyword()));
        if (currentUser.getRole() == Role.ADMIN) {
            spec = spec.and(TaskSpecification.hasUser(currentUser.getId()));

        }
        Page<Task> result = taskRepository.findAll(spec, pageable);
        return toPageResponse(result);

    }

    public TaskResponse getTaskById(Long id) {
        User currentUser = authUtils.getCurrentUser();

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        if (!task.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("You do not have permission to access this task");
        }
        return toResponse(task);
    }

    public TaskResponse createTask(TaskRequest request) {
        User currentUser = authUtils.getCurrentUser();
        log.info("Creating task title='{}' for user={}", request.getTitle(), currentUser.getEmail());
        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus())
                .user(currentUser)
                .build();

        return toResponse(taskRepository.save(task));

    }

    public TaskResponse updateTask(Long id, TaskRequest request) {
        User currentUser = authUtils.getCurrentUser();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        if (!task.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("You do not have permission to update this task");
        }
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());

        return toResponse(taskRepository.save(task));
    }

    public void deleteTask(Long id) {
        User currentUser = authUtils.getCurrentUser();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        if (!task.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("You do not have permission to delete this task");
        }
        taskRepository.deleteById(id);
        log.info("Task deleted id={} by user={}", id, currentUser.getEmail());
    }
}
