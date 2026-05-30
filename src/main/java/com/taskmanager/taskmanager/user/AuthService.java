package com.taskmanager.taskmanager.user;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskmanager.taskmanager.auth.RefreshToken;
import com.taskmanager.taskmanager.auth.RefreshTokenService;
import com.taskmanager.taskmanager.config.JwtService;
import com.taskmanager.taskmanager.exception.BadRequestException;
import com.taskmanager.taskmanager.exception.UnauthorizedException;
import com.taskmanager.taskmanager.rbac.Role;
import com.taskmanager.taskmanager.rbac.RoleRepository;
import com.taskmanager.taskmanager.user.dto.AuthResponse;
import com.taskmanager.taskmanager.user.dto.LoginRequest;
import com.taskmanager.taskmanager.user.dto.RegisterRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RoleRepository roleRepository;
    private final RefreshTokenService refreshTokenService;
    private static final String DUMMY_HASH = "$2a$10$dummyhashusedtopreventtimingattacksxxxxxxxxxxxxxxxxxxxxx";

    // ─── Build response ────────────────────────────────────────────────
    private AuthResponse buildAuthResponse(User user,
            String accessToken,
            String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .name(user.getName())
                .email(user.getEmail())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .permissions(user.getAllPermissions())
                .build();
    }

    // ─── Register ──────────────────────────────────────────────────────
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new BadRequestException(
                        "Default role not found. Contact admin."));

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(new HashSet<>(Set.of(userRole)))
                .build();

        userRepository.save(user);
        log.info("User registered: {}", user.getEmail());

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken.getToken());
    }

    // ─── Login ─────────────────────────────────────────────────────────
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(), request.getPassword()));
        } catch (Exception e) {
            passwordEncoder.matches("dummy", DUMMY_HASH);
            // log.warn("Failed login attempt for email={}", request.getEmail());
            if (user != null) {
                handleFailedLogin(user);
            }
            throw new UnauthorizedException("Invalid credentials");
        }

        if (user != null && user.isAccountLocked()) {
            throw new UnauthorizedException("Account is locked due to too many failed attempts. Try again later.");
        }
        if (user != null && user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user, accessToken, refreshToken.getToken());
    }

    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= 5) {
            // lock for 15 mins after 5 attempts
            user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
            log.warn("Account locked for user={} after {} failed attempts",
                    user.getEmail(), attempts);
        }
        userRepository.save(user);
    }

    // ─── Refresh ───────────────────────────────────────────────────────
    @Transactional
    public AuthResponse refresh(String refreshTokenStr) {
        // Validate the incoming refresh token
        RefreshToken refreshToken = refreshTokenService
                .validateRefreshToken(refreshTokenStr);

        User user = refreshToken.getUser();

        // Rotate — revoke old token, issue new one
        RefreshToken newRefreshToken = refreshTokenService
                .rotateRefreshToken(refreshToken);

        // Issue new access token
        String newAccessToken = jwtService.generateToken(user);

        log.info("Tokens rotated for user={}", user.getEmail());
        return buildAuthResponse(user, newAccessToken, newRefreshToken.getToken());
    }

    // ─── Logout ────────────────────────────────────────────────────────
    @Transactional
    public void logout(String refreshTokenStr) {
        RefreshToken refreshToken = refreshTokenService
                .validateRefreshToken(refreshTokenStr);
        refreshTokenService.revokeAllUserTokens(refreshToken.getUser());
        log.info("User logged out: {}", refreshToken.getUser().getEmail());
    }
}