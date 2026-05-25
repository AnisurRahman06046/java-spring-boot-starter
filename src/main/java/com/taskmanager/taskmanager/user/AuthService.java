package com.taskmanager.taskmanager.user;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.taskmanager.taskmanager.config.JwtService;
import com.taskmanager.taskmanager.exception.BadRequestException;
import com.taskmanager.taskmanager.exception.UnauthorizedException;
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

    public AuthResponse register(RegisterRequest request) {
        // check if email already exist
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        userRepository.save(user);

        return AuthResponse.builder()
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (Exception e) {
            // TODO: handle exception
            throw new UnauthorizedException("Invalid credentials");
        }
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        ;
        String token = jwtService.generateToken(user);
        return AuthResponse.builder().token(token).role(user.getRole().name()).build();

    }
}
