package com.taskmanager.taskmanager.task;

import java.util.List;

import org.springframework.stereotype.Service;

import com.taskmanager.taskmanager.config.AuthUtils;
import com.taskmanager.taskmanager.exception.ResourceNotFoundException;
import com.taskmanager.taskmanager.exception.UnauthorizedException;
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

    // map entity to dto 
    private TaskResponse toResponse(Task task){
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

    public List<TaskResponse> getAllTasks(){
        User currentUser = authUtils.getCurrentUser();
        if(currentUser.getRole()==Role.ADMIN){
            return taskRepository.findAll().stream().map(this::toResponse).toList();
        }
        return taskRepository.findByUserId(currentUser.getId())
                            .stream()
                            .map(this::toResponse)
                            .toList();
    }

    public TaskResponse getTaskById(Long id) {
        User currentUser = authUtils.getCurrentUser();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        
        if(!task.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN){
            throw new UnauthorizedException("You do not have permission to access this task");
        }
        return toResponse(task);
    }

    public TaskResponse createTask(TaskRequest request){
        User currentUser = authUtils.getCurrentUser();
        Task task = Task.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .status(request.getStatus())
            .user(currentUser)
            .build();
        
        return toResponse(taskRepository.save(task));

    }

    public TaskResponse updateTask(Long id,TaskRequest request){
        User currentUser = authUtils.getCurrentUser();
        Task task = taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        if(!task.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN){
            throw new UnauthorizedException("You do not have permission to update this task");
        }
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());

        return toResponse(taskRepository.save(task));
    }

    public void deleteTask(Long id){
        User currentUser = authUtils.getCurrentUser();
        Task task = taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        if(!task.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN){
            throw new UnauthorizedException("You do not have permission to delete this task");
        }
        taskRepository.deleteById(id);
    }
}
