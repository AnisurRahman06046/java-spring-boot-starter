package com.taskmanager.taskmanager.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.taskmanager.taskmanager.rbac.Permission;
import com.taskmanager.taskmanager.rbac.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private int failedLoginAttempts = 0;
    private LocalDateTime lockedUntil;

    // User ↔ Role (many-to-many)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ─── Helper: get all permission names across all roles ────────────
    // e.g. ["TASK:READ", "TASK:CREATE", "ROLE:MANAGE"]
    @JsonIgnore
    public Set<String> getAllPermissions() {
        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());
    }

    // ─── Helper: check if user has a specific permission ──────────────
    public boolean hasPermission(String permissionName) {
        return getAllPermissions().contains(permissionName);
    }

    // ─── Helper: check if user has a specific role name ───────────────
    public boolean hasRole(String roleName) {
        return roles.stream()
                .anyMatch(r -> r.getName().equalsIgnoreCase(roleName));
    }

    // ─── UserDetails — Spring Security reads authorities from here ────
    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Give Spring Security the permission strings as authorities
        // This is what @PreAuthorize("hasAuthority('TASK:READ')") checks
        Set<GrantedAuthority> authorities = getAllPermissions().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

        // Also add role names prefixed with ROLE_ for hasRole() checks
        roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getName()))
                .forEach(authorities::add);

        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    public boolean isAccountLocked() {
        return lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil);
    }

    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return enabled;
    }
}