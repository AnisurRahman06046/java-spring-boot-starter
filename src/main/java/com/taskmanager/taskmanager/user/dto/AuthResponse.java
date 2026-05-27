package com.taskmanager.taskmanager.user.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.Set;

@Getter
@Builder
public class AuthResponse {
    private String token;
    private String name;
    private String email;
    private Set<String> roles; // e.g. ["USER", "MANAGER"]
    private Set<String> permissions; // e.g. ["TASK:READ", "TASK:CREATE"]
}