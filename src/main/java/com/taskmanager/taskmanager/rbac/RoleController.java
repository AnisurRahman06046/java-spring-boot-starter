package com.taskmanager.taskmanager.rbac;

import com.taskmanager.taskmanager.common.ApiResponse;
import com.taskmanager.taskmanager.rbac.dto.AssignRoleRequest;
import com.taskmanager.taskmanager.rbac.dto.CreateRoleRequest;
import com.taskmanager.taskmanager.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    // ── Anyone with ROLE:MANAGE can create roles ──────────────────────
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE:MANAGE')")
    public ResponseEntity<ApiResponse<Role>> createRole(
            @Valid @RequestBody CreateRoleRequest request) {
        Role role = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Role created successfully", role));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE:MANAGE')")
    public ResponseEntity<ApiResponse<List<Role>>> getAllRoles() {
        return ResponseEntity.ok(
                ApiResponse.success("Roles fetched", roleService.getAllRoles()));
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('ROLE:MANAGE')")
    public ResponseEntity<ApiResponse<List<Permission>>> getAllPermissions() {
        return ResponseEntity.ok(
                ApiResponse.success("Permissions fetched", roleService.getAllPermissions()));
    }

    @PostMapping("/assign/{userId}")
    @PreAuthorize("hasAuthority('ROLE:MANAGE')")
    public ResponseEntity<ApiResponse<String>> assignRoles(
            @PathVariable Long userId,
            @Valid @RequestBody AssignRoleRequest request) {
        User user = roleService.assignRolesToUser(userId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Roles assigned to " + user.getEmail()));
    }

    @DeleteMapping("/assign/{userId}")
    @PreAuthorize("hasAuthority('ROLE:MANAGE')")
    public ResponseEntity<ApiResponse<String>> removeRoles(
            @PathVariable Long userId,
            @Valid @RequestBody AssignRoleRequest request) {
        User user = roleService.removeRolesFromUser(userId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Roles removed from " + user.getEmail()));
    }
}