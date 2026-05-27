package com.taskmanager.taskmanager.rbac;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskmanager.taskmanager.config.AuthUtils;
import com.taskmanager.taskmanager.exception.BadRequestException;
import com.taskmanager.taskmanager.exception.ResourceNotFoundException;
import com.taskmanager.taskmanager.rbac.dto.AssignRoleRequest;
import com.taskmanager.taskmanager.rbac.dto.CreateRoleRequest;
import com.taskmanager.taskmanager.user.User;
import com.taskmanager.taskmanager.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleService {
    private static final Logger log = LoggerFactory.getLogger(RoleService.class);
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final AuthUtils authUtils;

    @Transactional
    public Role createRole(CreateRoleRequest request) {
        if (roleRepository.existsByName(request.getName().toUpperCase())) {
            throw new BadRequestException("Role already exists: " + request.getName());
        }

        // validate requested permissions exists
        Set<Permission> permissions = permissionRepository.findByNameIn(request.getPermissions());

        if (permissions.size() != request.getPermissions().size()) {
            Set<String> found = permissions.stream()
                    .map(Permission::getName).collect(Collectors.toSet());
            Set<String> invalid = new HashSet<>(request.getPermissions());
            invalid.removeAll(found);
            throw new BadRequestException("Invalid permissions: " + invalid);
        }
        // ── Privilege escalation prevention ───────────────────────────
        // You can only create a role with permissions you yourself have
        User currentUser = authUtils.getCurrentUser();
        if (!currentUser.hasRole("SUPER_ADMIN")) {
            Set<String> myPermissions = currentUser.getAllPermissions();
            Set<String> notAllowed = request.getPermissions().stream()
                    .filter(p -> !myPermissions.contains(p))
                    .collect(Collectors.toSet());
            if (!notAllowed.isEmpty()) {
                throw new BadRequestException(
                        "You cannot grant permissions you don't have: " + notAllowed);
            }
        }

        Role role = Role.builder().name(request.getName().toUpperCase()).description(request.getDescription())
                .permissions(permissions).build();

        Role saved = roleRepository.save(role);
        log.info("Role created: {} by user={}",
                saved.getName(), currentUser.getEmail());
        return saved;
    }

    // ─── Assign roles to a user ───────────────────────────────────────
    @Transactional
    public User assignRolesToUser(Long userId, AssignRoleRequest request) {
        User currentUser = authUtils.getCurrentUser();
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Set<Role> rolesToAssign = request.getRoles().stream()
                .map(name -> roleRepository.findByName(name.toUpperCase())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Role not found: " + name)))
                .collect(Collectors.toSet());

        // ── Privilege escalation prevention ───────────────────────────
        // You can only assign roles whose permissions you yourself have
        if (!currentUser.hasRole("SUPER_ADMIN")) {
            Set<String> myPermissions = currentUser.getAllPermissions();
            rolesToAssign.forEach(role -> role.getPermissions().forEach(permission -> {
                if (!myPermissions.contains(permission.getName())) {
                    throw new BadRequestException(
                            "You cannot assign role '" + role.getName() +
                                    "' — it contains permission '" + permission.getName() +
                                    "' which you don't have");
                }
            }));
        }

        targetUser.getRoles().addAll(rolesToAssign);
        User saved = userRepository.save(targetUser);

        log.info("Roles {} assigned to user={} by admin={}",
                request.getRoles(), targetUser.getEmail(), currentUser.getEmail());
        return saved;
    }

    // ─── Remove roles from a user ─────────────────────────────────────
    @Transactional
    public User removeRolesFromUser(Long userId, AssignRoleRequest request) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        targetUser.getRoles().removeIf(role -> request.getRoles().contains(role.getName()));

        return userRepository.save(targetUser);
    }

    // ─── List all roles ───────────────────────────────────────────────
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    // ─── List all permissions ─────────────────────────────────────────
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }
}
