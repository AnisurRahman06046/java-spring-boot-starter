package com.taskmanager.taskmanager.task.dto;

import com.taskmanager.taskmanager.task.TaskStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskFilterRequest {
    // filtering
    private TaskStatus status;
    private String keyword;

    // pagination
    private int page = 0;
    private int size = 10;

    // sorting
    private String sortBy = "createdAt";
    private String sortDir = "desc";
}
