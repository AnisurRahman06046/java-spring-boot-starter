package com.taskmanager.taskmanager.rbac.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import java.util.Set;

@Getter
@Setter
public class AssignRoleRequest {

    @NotEmpty(message = "At least one role is required")
    private Set<String> roles; // e.g. ["MANAGER", "BILLING_VIEWER"]
}