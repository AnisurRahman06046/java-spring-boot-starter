package com.taskmanager.taskmanager.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL) // hides all null fields from json
public class ApiResponse <T>{
    
    private final boolean success;
    private final String message;
    private final T data;

    private ApiResponse(boolean success,String message, T data){
        this.success=success;
        this.message=message;
        this.data=data;
    }


    // success with data
    public static <T> ApiResponse <T> success (String message,T data){
        return new ApiResponse<>(true, message, data);
    }

    // success without data - delete response
    public static <T> ApiResponse <T> success(String message){
        return new ApiResponse <>(true, message, null);
    }

    // error
    public static <T> ApiResponse <T>  error(String message){
        return new ApiResponse <>(false, message, null);
    }
}
