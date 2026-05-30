package com.taskmanager.taskmanager.auth;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskmanager.taskmanager.exception.UnauthorizedException;
import com.taskmanager.taskmanager.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    // Max concurrent sessions per user — prevents token flooding
    private static final int MAX_ACTIVE_SESSIONS = 5;

    // ─── Create and store a new refresh token ─────────────────────────
    @Transactional
    public RefreshToken createRefreshToken(User user, String userAgent) {

        // Enforce max sessions — revoke oldest if limit exceeded
        long activeTokens = refreshTokenRepository.countByUserAndRevokedFalse(user);
        if (activeTokens >= MAX_ACTIVE_SESSIONS) {
            log.warn("Max sessions reached for user={}, revoking all old tokens",
                    user.getEmail());
            refreshTokenRepository.deleteByUser(user);
        }

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString()) // random, unguessable
                .user(user)
                .expiresAt(Instant.now().plusMillis(refreshExpirationMs))
                .userAgent(userAgent)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    // ─── Validate and return token — throws if invalid ────────────────
    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            // Someone is using a revoked token — possible token theft
            log.warn("Revoked refresh token used for user={}. Revoking ALL sessions.",
                    refreshToken.getUser().getEmail());
            // Security: revoke ALL tokens for this user
            refreshTokenRepository.deleteByUser(refreshToken.getUser());
            throw new UnauthorizedException(
                    "Refresh token was revoked. Please login again.");
        }

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new UnauthorizedException("Refresh token expired. Please login again.");
        }

        return refreshToken;
    }

    // ─── Rotate token — revoke old, issue new (security best practice)
    @Transactional
    public RefreshToken rotateRefreshToken(RefreshToken oldToken, String userAgent) {
        // Mark old token as revoked
        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);

        // Issue brand new token
        return createRefreshToken(oldToken.getUser(), userAgent);
    }

    // ─── Logout — revoke all tokens for user ──────────────────────────
    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.deleteByUser(user);
        log.info("All refresh tokens revoked for user={}", user.getEmail());
    }
}