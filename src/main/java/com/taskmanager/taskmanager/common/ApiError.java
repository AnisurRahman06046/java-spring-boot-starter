package com.taskmanager.taskmanager.common;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
    
    private final boolean success = false;
    private final int status;
    private final String message;
    private final Map<String,String>errors;
    private final LocalDateTime timestamp;

    public static ApiError of(int status,String message){
        return ApiError.builder()
                .status(status)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ApiError of(int status,String message,Map<String,String>errors){
        return ApiError.builder()
            .status(status)
            .message(message)
            .errors(errors)
            .timestamp(LocalDateTime.now())
            .build();
    }
}
