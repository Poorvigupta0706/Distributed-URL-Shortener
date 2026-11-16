package com.poorvi.url_shortener.auth.service;

import com.poorvi.url_shortener.auth.dto.RegisterRequest;
import com.poorvi.url_shortener.auth.dto.AuthResponse;
import com.poorvi.url_shortener.auth.dto.LoginRequest;
import com.poorvi.url_shortener.security.JwtService;
import com.poorvi.url_shortener.user.entity.user;
import com.poorvi.url_shortener.user.repository.UserRepository;

import jakarta.validation.Valid;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // User Registration
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        user user1 = user.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user1);

        return new AuthResponse(null, "User registered successfully");
    }

    // User Login
    public AuthResponse login(LoginRequest request) {

        user user1 = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user1.getPassword())) {

            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = jwtService.generateToken(user1.getEmail());

        return new AuthResponse(token, "Login successful");
    }

    public AuthResponse Register(RegisterRequest request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'Register'");
    }
}