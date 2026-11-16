package com.poorvi.url_shortener.auth.controller;

import com.poorvi.url_shortener.auth.dto.RegisterRequest;
import com.poorvi.url_shortener.auth.dto.AuthResponse;
import com.poorvi.url_shortener.auth.service.AuthService;
import com.poorvi.url_shortener.auth.dto.LoginRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
        public ResponseEntity<AuthResponse> register(
                @Valid @RequestBody RegisterRequest request) {

            AuthResponse response=authService.register(request);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);
    }
    

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}