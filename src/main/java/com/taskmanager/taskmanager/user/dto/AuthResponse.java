package com.taskmanager.taskmanager.user.dto;

import java.util.Set;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String name;
    private String email;
    private Set<String> roles; // e.g. ["USER", "MANAGER"]
    private Set<String> permissions; // e.g. ["TASK:READ", "TASK:CREATE"]
}