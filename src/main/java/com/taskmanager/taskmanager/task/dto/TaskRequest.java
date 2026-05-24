package com.taskmanager.taskmanager.task.dto;

import com.taskmanager.taskmanager.common.validation.NoTestWord;
import com.taskmanager.taskmanager.task.TaskStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskRequest {
    @NotBlank(message="Title is required")
    @Size(min=3,max=100,message= "Title must be between 3 and 100 characters")
    @NoTestWord
    private String title;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Status is required")
    private TaskStatus status;
}


/*@NotBlank vs @NotNull vs @NotEmpty:

@NotNull — value can't be null
@NotEmpty — can't be null or empty string ""
@NotBlank — can't be null, empty, or just whitespace "   " ← use this for strings */

/*// Strings
@NotNull        // not null
@NotBlank       // not null, not empty, not whitespace
@Size(min, max) // length range
@Email          // valid email format
@Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Only alphanumeric allowed")

// Numbers
@Min(1)         // minimum value
@Max(100)       // maximum value
@Positive       // must be > 0
@PositiveOrZero // must be >= 0

// Dates
@Future         // must be a future date
@Past           // must be a past date
@FutureOrPresent

// Collections
@NotEmpty       // list/array can't be empty
@Size(min=1, max=10) // list size range */