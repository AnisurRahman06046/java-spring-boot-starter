package com.taskmanager.taskmanager.config;

import com.taskmanager.taskmanager.rbac.Permission;
import com.taskmanager.taskmanager.rbac.PermissionRepository;
import com.taskmanager.taskmanager.rbac.Role;
import com.taskmanager.taskmanager.rbac.RoleRepository;
import com.taskmanager.taskmanager.user.User;
import com.taskmanager.taskmanager.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.super-admin.email:superadmin@taskmanager.com}")
    private String superAdminEmail;

    @Value("${app.super-admin.password:SuperAdmin@123}")
    private String superAdminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Running DataInitializer...");
        seedPermissions();
        Role superAdminRole = seedSuperAdminRole();
        Role userRole = seedDefaultUserRole();
        seedSuperAdminUser(superAdminRole);
        log.info("DataInitializer complete.");
    }

    // ─── 1. Seed all system permissions ───────────────────────────────
    private void seedPermissions() {
        Map<String, String> permissions = Map.of(
                Permission.TASK_READ, "Can read tasks",
                Permission.TASK_CREATE, "Can create tasks",
                Permission.TASK_UPDATE, "Can update tasks",
                Permission.TASK_DELETE, "Can delete tasks",
                Permission.USER_READ, "Can read users",
                Permission.USER_MANAGE, "Can manage users",
                Permission.ROLE_MANAGE, "Can manage roles and permissions");

        permissions.forEach((name, description) -> {
            if (!permissionRepository.existsByName(name)) {
                permissionRepository.save(
                        Permission.builder()
                                .name(name)
                                .description(description)
                                .build());
                log.info("Seeded permission: {}", name);
            }
        });
    }

    // ─── 2. Seed SUPER_ADMIN role with ALL permissions ─────────────────
    private Role seedSuperAdminRole() {
        return roleRepository.findByName("SUPER_ADMIN").orElseGet(() -> {
            Set<Permission> allPermissions = Set.copyOf(permissionRepository.findAll());
            Role role = Role.builder()
                    .name("SUPER_ADMIN")
                    .description("Full system access")
                    .permissions(allPermissions)
                    .build();
            log.info("Seeded role: SUPER_ADMIN");
            return roleRepository.save(role);
        });
    }

    // ─── 3. Seed default USER role with basic permissions ─────────────
    private Role seedDefaultUserRole() {
        return roleRepository.findByName("USER").orElseGet(() -> {
            Set<Permission> basicPermissions = permissionRepository
                    .findByNameIn(Set.of(
                            Permission.TASK_READ,
                            Permission.TASK_CREATE,
                            Permission.TASK_UPDATE,
                            Permission.TASK_DELETE));
            Role role = Role.builder()
                    .name("USER")
                    .description("Standard user access")
                    .permissions(basicPermissions)
                    .build();
            log.info("Seeded role: USER");
            return roleRepository.save(role);
        });
    }

    // ─── 4. Seed the super admin user ─────────────────────────────────
    private void seedSuperAdminUser(Role superAdminRole) {
        if (!userRepository.existsByEmail(superAdminEmail)) {
            User superAdmin = User.builder()
                    .name("Super Admin")
                    .email(superAdminEmail)
                    .password(passwordEncoder.encode(superAdminPassword))
                    .roles(Set.of(superAdminRole))
                    .build();
            userRepository.save(superAdmin);
            log.info("Seeded SUPER_ADMIN user: {}", superAdminEmail);
        }
    }
}