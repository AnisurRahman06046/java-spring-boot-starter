package com.taskmanager.taskmanager.auth;

import java.time.Instant;

import com.taskmanager.taskmanager.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The actual token string — long random UUID
    @Column(nullable = false, unique = true)
    private String token;

    // Which user this token belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // When this token expires
    @Column(nullable = false)
    private Instant expiresAt;

    // Was this token already used? (rotation security)
    @Builder.Default
    private boolean revoked = false;

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}