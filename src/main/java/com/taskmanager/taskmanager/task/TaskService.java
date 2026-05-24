package com.taskmanager.taskmanager.task;

import org.springframework.stereotype.Service;

import com.taskmanager.taskmanager.task.dto.TaskRequest;
import com.taskmanager.taskmanager.task.dto.TaskResponse;

import lombok.RequiredArgsConstructor;

import java.util.List;

import com.taskmanager.taskmanager.exception.ResourceNotFoundException;

@Service
@RequiredArgsConstructor // auto generates constructor for final fields by lombork
public class TaskService {
    private final TaskRepository taskRepository;

    // map entity to dto 
    private TaskResponse toResponse(Task task){
        return TaskResponse.builder()
            .id(task.getId())
            .title(task.getTitle())
            .description(task.getDescription())
            .status(task.getStatus())
            .createdAt(task.getCreatedAt())
            .updatedAt(task.getUpdatedAt())
            .build();
    }

    public List<TaskResponse> getAllTasks(){
        return taskRepository.findAll()
                            .stream()
                            .map(this::toResponse)
                            .toList();
    }

    public TaskResponse getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        return toResponse(task);
    }

    public TaskResponse createTask(TaskRequest request){
        Task task = Task.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .status(request.getStatus())
            .build();
        
        return toResponse(taskRepository.save(task));

    }

    public TaskResponse updateTask(Long id,TaskRequest request){
        Task task = taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());

        return toResponse(taskRepository.save(task));
    }

    public void deleteTask(Long id){
        if(!taskRepository.existsById(id)){
            throw new ResourceNotFoundException("Task does not exist");
        }
        taskRepository.deleteById(id);
    }
}
