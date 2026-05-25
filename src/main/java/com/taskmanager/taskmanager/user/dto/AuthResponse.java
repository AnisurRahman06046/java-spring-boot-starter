package com.taskmanager.taskmanager.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String token;
    private String name;
    private String email;
    private String role;
}
