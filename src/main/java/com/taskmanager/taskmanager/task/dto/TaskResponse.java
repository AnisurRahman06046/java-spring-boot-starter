package com.taskmanager.taskmanager.task.dto;

import java.time.LocalDateTime;

import com.taskmanager.taskmanager.task.TaskStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
