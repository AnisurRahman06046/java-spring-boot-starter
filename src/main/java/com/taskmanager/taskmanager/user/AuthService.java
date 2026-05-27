package com.taskmanager.taskmanager.user;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.taskmanager.taskmanager.rbac.Role;
import com.taskmanager.taskmanager.config.JwtService;
import com.taskmanager.taskmanager.exception.BadRequestException;
import com.taskmanager.taskmanager.exception.UnauthorizedException;
import com.taskmanager.taskmanager.rbac.RoleRepository;
import com.taskmanager.taskmanager.user.dto.AuthResponse;
import com.taskmanager.taskmanager.user.dto.LoginRequest;
import com.taskmanager.taskmanager.user.dto.RegisterRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RoleRepository roleRepository;

    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .name(user.getName())
                .email(user.getEmail())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .permissions(user.getAllPermissions())
                .build();
    }

    public AuthResponse register(RegisterRequest request) {
        // check if email already exist
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        Role usRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new BadRequestException("Default role not found. Contact admin."));

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(new HashSet<>(Set.of(usRole)))
                .build();

        userRepository.save(user);
        String token = jwtService.generateToken(user);
        return buildAuthResponse(user, token);
    }

    // public AuthResponse login(LoginRequest request) {
    // try {
    // authenticationManager.authenticate(
    // new UsernamePasswordAuthenticationToken(
    // request.getEmail(), request.getPassword()));
    // } catch (Exception e) {
    // throw new UnauthorizedException("Invalid credentials");
    // }

    // // ← pass user, not null
    // User user = userRepository.findByEmail(request.getEmail())
    // .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

    // String token = jwtService.generateToken(user);
    // return buildAuthResponse(user, token); // ← was buildAuthResponse(null,
    // token)
    // }
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(), request.getPassword()));
        } catch (Exception e) {
            // TEMPORARY — log the real cause
            e.printStackTrace();
            System.out.println("AUTH FAILED CAUSE: " + e.getClass().getName() + " — " + e.getMessage());
            throw new UnauthorizedException("Invalid credentials");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        String token = jwtService.generateToken(user);
        return buildAuthResponse(user, token);
    }
}
